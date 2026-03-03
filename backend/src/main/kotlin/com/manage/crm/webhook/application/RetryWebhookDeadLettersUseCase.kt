package com.manage.crm.webhook.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.webhook.WebhookClient
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 200
    }

    @Transactional
    suspend fun retrySingle(webhookId: Long, deadLetterId: Long): WebhookDeadLetterRetryResultDto {
        val webhook = webhookRepository.findById(webhookId) ?: throw NotFoundByIdException("Webhook", webhookId)
        val deadLetter = webhookDeadLetterRepository.findById(deadLetterId) ?: throw NotFoundByIdException("WebhookDeadLetter", deadLetterId)
        if (deadLetter.webhookId != webhookId) {
            throw IllegalArgumentException("Dead letter $deadLetterId does not belong to webhook $webhookId")
        }
        return retryDeadLetter(webhook, deadLetter)
    }

    @Transactional
    suspend fun retryBatch(useCaseIn: RetryWebhookDeadLettersUseCaseIn): RetryWebhookDeadLettersUseCaseOut {
        val webhook = webhookRepository.findById(useCaseIn.webhookId)
            ?: throw NotFoundByIdException("Webhook", useCaseIn.webhookId)

        val normalizedLimit = useCaseIn.limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        val targets = if (useCaseIn.deadLetterIds.isNotEmpty()) {
            useCaseIn.deadLetterIds.distinct().mapNotNull { deadLetterId ->
                webhookDeadLetterRepository.findById(deadLetterId)
                    ?.takeIf { it.webhookId == useCaseIn.webhookId }
            }
        } else {
            webhookDeadLetterRepository.findByWebhookIdOrderByCreatedAtDesc(useCaseIn.webhookId)
                .filter { it.deliveryStatus != "REDRIVEN_SUCCESS" }
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
        deadLetter: WebhookDeadLetter
    ): WebhookDeadLetterRetryResultDto {
        val payload = runCatching {
            objectMapper.readValue(deadLetter.payloadJson, WebhookEventPayload::class.java)
        }.getOrElse { error ->
            deadLetter.deliveryStatus = "REDRIVE_PARSE_FAILED"
            deadLetter.errorMessage = error.message
            webhookDeadLetterRepository.save(deadLetter)
            return WebhookDeadLetterRetryResultDto(
                deadLetterId = requireNotNull(deadLetter.id) { "deadLetter id cannot be null" },
                webhookId = deadLetter.webhookId,
                eventId = deadLetter.eventId,
                status = deadLetter.deliveryStatus,
                attemptCount = deadLetter.attemptCount,
                responseStatus = deadLetter.responseStatus,
                errorMessage = deadLetter.errorMessage
            )
        }

        val result = webhookClient.send(webhook, payload)
        saveDeliveryLog(deadLetter.webhookId, payload, result)

        val nextStatus = if (result.status == WebhookDeliveryStatus.SUCCESS) {
            "REDRIVEN_SUCCESS"
        } else {
            "REDRIVE_FAILED"
        }
        deadLetter.deliveryStatus = nextStatus
        deadLetter.attemptCount = deadLetter.attemptCount + result.attemptCount
        deadLetter.responseStatus = result.responseStatus
        deadLetter.errorMessage = result.errorMessage
        val updatedDeadLetter = webhookDeadLetterRepository.save(deadLetter)

        return WebhookDeadLetterRetryResultDto(
            deadLetterId = requireNotNull(updatedDeadLetter.id) { "deadLetter id cannot be null" },
            webhookId = updatedDeadLetter.webhookId,
            eventId = updatedDeadLetter.eventId,
            status = updatedDeadLetter.deliveryStatus,
            attemptCount = updatedDeadLetter.attemptCount,
            responseStatus = updatedDeadLetter.responseStatus,
            errorMessage = updatedDeadLetter.errorMessage
        )
    }

    private suspend fun saveDeliveryLog(
        webhookId: Long,
        payload: WebhookEventPayload,
        deliveryResult: com.manage.crm.webhook.WebhookDeliveryResult
    ) {
        webhookDeliveryLogRepository.save(
            WebhookDeliveryLog.new(
                webhookId = webhookId,
                eventId = payload.eventId,
                eventType = payload.eventType,
                deliveryStatus = deliveryResult.status.name,
                attemptCount = deliveryResult.attemptCount,
                responseStatus = deliveryResult.responseStatus,
                errorMessage = deliveryResult.errorMessage
            )
        )
    }
}
