package com.manage.crm.webhook.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.webhook.WebhookClient
import com.manage.crm.webhook.WebhookDeliveryResult
import com.manage.crm.webhook.WebhookDeliveryStatus
import com.manage.crm.webhook.application.dto.RetryWebhookDeadLettersUseCaseIn
import com.manage.crm.webhook.application.dto.RetryWebhookDeadLettersUseCaseOut
import com.manage.crm.webhook.application.dto.WebhookDeadLetterRetryResultDto
import com.manage.crm.webhook.domain.WebhookDeadLetter
import com.manage.crm.webhook.domain.WebhookDeliveryLog
import com.manage.crm.webhook.domain.WebhookEventPayload
import com.manage.crm.webhook.domain.repository.WebhookDeadLetterRepository
import com.manage.crm.webhook.domain.repository.WebhookDeliveryLogRepository
import com.manage.crm.webhook.domain.repository.WebhookRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

/**
 * Re-drives webhook dead-letter events either in batch or per item.
 */
@Service
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
class RetryWebhookDeadLettersUseCase(
    private val webhookRepository: WebhookRepository,
    private val webhookDeadLetterRepository: WebhookDeadLetterRepository,
    private val webhookDeliveryLogRepository: WebhookDeliveryLogRepository,
    private val webhookClient: WebhookClient,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 200
    }

    suspend fun retrySingle(
        webhookId: Long,
        deadLetterId: Long,
    ): WebhookDeadLetterRetryResultDto {
        val webhook = webhookRepository.findById(webhookId) ?: throw NotFoundByIdException("Webhook", webhookId)
        val deadLetter =
            webhookDeadLetterRepository.findById(deadLetterId) ?: throw NotFoundByIdException("WebhookDeadLetter", deadLetterId)
        if (deadLetter.webhookId != webhookId) {
            throw IllegalArgumentException("Dead letter $deadLetterId does not belong to webhook $webhookId")
        }
        return retryDeadLetter(webhook, deadLetter)
    }

    suspend fun retryBatch(useCaseIn: RetryWebhookDeadLettersUseCaseIn): RetryWebhookDeadLettersUseCaseOut {
        val webhook =
            webhookRepository.findById(useCaseIn.webhookId)
                ?: throw NotFoundByIdException("Webhook", useCaseIn.webhookId)

        val normalizedLimit = useCaseIn.limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        val targets =
            if (useCaseIn.deadLetterIds.isNotEmpty()) {
                val validDeadLetters = mutableListOf<WebhookDeadLetter>()
                val invalidDeadLetterIds = mutableListOf<Long>()

                useCaseIn.deadLetterIds.distinct().forEach { deadLetterId ->
                    val deadLetter = webhookDeadLetterRepository.findById(deadLetterId)
                    if (deadLetter == null || deadLetter.webhookId != useCaseIn.webhookId) {
                        invalidDeadLetterIds += deadLetterId
                    } else {
                        validDeadLetters += deadLetter
                    }
                }

                if (invalidDeadLetterIds.isNotEmpty()) {
                    throw IllegalArgumentException(
                        "Invalid deadLetterIds for webhookId=${useCaseIn.webhookId}: ${invalidDeadLetterIds.joinToString(",")}",
                    )
                }

                validDeadLetters
            } else {
                webhookDeadLetterRepository
                    .findByWebhookIdOrderByCreatedAtDesc(useCaseIn.webhookId)
                    .filter { it.deliveryStatus != "REDRIVEN_SUCCESS" && it.deliveryStatus != "REDRIVING" }
                    .take(normalizedLimit)
                    .toList()
            }

        val results = mutableListOf<WebhookDeadLetterRetryResultDto>()
        for (deadLetter in targets) {
            results += retryDeadLetter(webhook, deadLetter)
        }

        return out {
            RetryWebhookDeadLettersUseCaseOut(results = results)
        }
    }

    private suspend fun retryDeadLetter(
        webhook: com.manage.crm.webhook.domain.Webhook,
        deadLetter: WebhookDeadLetter,
    ): WebhookDeadLetterRetryResultDto {
        val deadLetterId = requireNotNull(deadLetter.id) { "deadLetter id cannot be null" }
        if (deadLetter.deliveryStatus == "REDRIVING") {
            throw IllegalStateException(
                "deadLetterId=$deadLetterId is in REDRIVING state from a previous incomplete persistence. " +
                    "Resolve state before retrying to avoid duplicate delivery.",
            )
        }

        val payload =
            runCatching {
                objectMapper.readValue(deadLetter.payloadJson, WebhookEventPayload::class.java)
            }.getOrElse { error ->
                deadLetter.deliveryStatus = "REDRIVE_PARSE_FAILED"
                deadLetter.attemptCount = deadLetter.attemptCount + 1
                deadLetter.responseStatus = null
                deadLetter.errorMessage = error.message
                webhookDeliveryLogRepository.save(
                    WebhookDeliveryLog.new(
                        webhookId = deadLetter.webhookId,
                        eventId = deadLetter.eventId,
                        eventType = deadLetter.eventType,
                        deliveryStatus = deadLetter.deliveryStatus,
                        attemptCount = 1,
                        responseStatus = null,
                        errorMessage = error.message,
                    ),
                )
                val updatedDeadLetter = webhookDeadLetterRepository.save(deadLetter)
                return toRetryResultDto(updatedDeadLetter)
            }

        deadLetter.deliveryStatus = "REDRIVING"
        deadLetter.attemptCount = deadLetter.attemptCount + 1
        deadLetter.responseStatus = null
        deadLetter.errorMessage = null
        val persistedDeadLetter = webhookDeadLetterRepository.save(deadLetter)

        val result = webhookClient.send(webhook, payload)
        runCatching {
            saveDeliveryLog(persistedDeadLetter.webhookId, payload, result)
        }.onFailure { error ->
            log.error(error) {
                "Webhook redrive delivery log persist failed after send: deadLetterId=$deadLetterId"
            }
        }.getOrElse { error ->
            throw IllegalStateException(
                "Webhook redrive delivered but failed to persist delivery log for deadLetterId=$deadLetterId. " +
                    "State remains REDRIVING to prevent duplicate resend.",
                error,
            )
        }

        val nextStatus =
            if (result.status == WebhookDeliveryStatus.SUCCESS) {
                "REDRIVEN_SUCCESS"
            } else {
                "REDRIVE_FAILED"
            }
        persistedDeadLetter.deliveryStatus = nextStatus
        persistedDeadLetter.attemptCount =
            persistedDeadLetter.attemptCount + (result.attemptCount - 1).coerceAtLeast(0)
        persistedDeadLetter.responseStatus = result.responseStatus
        persistedDeadLetter.errorMessage = result.errorMessage
        val updatedDeadLetter =
            runCatching {
                webhookDeadLetterRepository.save(persistedDeadLetter)
            }.onFailure { error ->
                log.error(error) {
                    "Webhook redrive final state persist failed after send: deadLetterId=$deadLetterId"
                }
            }.getOrElse { error ->
                throw IllegalStateException(
                    "Webhook redrive delivered but failed to persist completion state for deadLetterId=$deadLetterId. " +
                        "State remains REDRIVING to prevent duplicate resend.",
                    error,
                )
            }

        return toRetryResultDto(updatedDeadLetter)
    }

    private suspend fun saveDeliveryLog(
        webhookId: Long,
        payload: WebhookEventPayload,
        deliveryResult: WebhookDeliveryResult,
    ) {
        webhookDeliveryLogRepository.save(
            WebhookDeliveryLog.new(
                webhookId = webhookId,
                eventId = payload.eventId,
                eventType = payload.eventType,
                deliveryStatus = deliveryResult.status.name,
                attemptCount = deliveryResult.attemptCount,
                responseStatus = deliveryResult.responseStatus,
                errorMessage = deliveryResult.errorMessage,
            ),
        )
    }

    private fun toRetryResultDto(deadLetter: WebhookDeadLetter): WebhookDeadLetterRetryResultDto =
        WebhookDeadLetterRetryResultDto(
            deadLetterId = requireNotNull(deadLetter.id) { "deadLetter id cannot be null" },
            webhookId = deadLetter.webhookId,
            eventId = deadLetter.eventId,
            status = deadLetter.deliveryStatus,
            attemptCount = deadLetter.attemptCount,
            responseStatus = deadLetter.responseStatus,
            errorMessage = deadLetter.errorMessage,
        )
}
