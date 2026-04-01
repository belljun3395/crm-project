package com.manage.crm.webhook.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.support.coroutine.eventListenerCoroutineScope
import com.manage.crm.webhook.WebhookClient
import com.manage.crm.webhook.WebhookDeliveryResult
import com.manage.crm.webhook.WebhookDeliveryStatus
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookDeadLetter
import com.manage.crm.webhook.domain.WebhookDeliveryLog
import com.manage.crm.webhook.domain.WebhookEventPayload
import com.manage.crm.webhook.domain.WebhookEventType
import com.manage.crm.webhook.domain.repository.WebhookDeadLetterRepository
import com.manage.crm.webhook.domain.repository.WebhookDeliveryLogRepository
import com.manage.crm.webhook.domain.repository.WebhookRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
class WebhookDispatchService(
    private val webhookRepository: WebhookRepository,
    private val webhookClient: WebhookClient,
    private val webhookDeliveryLogRepository: WebhookDeliveryLogRepository,
    private val webhookDeadLetterRepository: WebhookDeadLetterRepository,
    private val objectMapper: ObjectMapper,
    private val webhookFailureAlertService: WebhookFailureAlertService,
) {
    private val log = KotlinLogging.logger {}

    suspend fun dispatch(
        eventType: WebhookEventType,
        payload: Map<String, Any?>,
    ) {
        val webhooks = webhookRepository.findActiveByEvent(eventType.value)
        if (webhooks.isEmpty()) {
            return
        }

        val eventPayload =
            WebhookEventPayload(
                eventId = UUID.randomUUID().toString(),
                eventType = eventType.value,
                occurredAt =
                    java.time.OffsetDateTime
                        .now()
                        .toString(),
                data = payload,
            )

        eventListenerCoroutineScope().apply {
            webhooks.forEach { webhook ->
                launch {
                    val webhookId = webhook.id
                    if (webhookId == null) {
                        log.error { "Skip webhook dispatch due to missing webhook id: name=${webhook.name}" }
                        return@launch
                    }

                    val deliveryResult = webhookClient.send(webhook, eventPayload)
                    saveDeliveryLog(webhookId, eventPayload, deliveryResult)

                    if (deliveryResult.status != WebhookDeliveryStatus.SUCCESS) {
                        saveDeadLetter(webhook, webhookId, eventPayload, deliveryResult)
                    }
                    runCatching {
                        webhookFailureAlertService.onDeliveryResult(webhook, deliveryResult)
                    }.onFailure { error ->
                        log.error(error) { "Failed to process webhook failure alert: webhookId=$webhookId" }
                    }
                }
            }
        }
    }

    private suspend fun saveDeliveryLog(
        webhookId: Long,
        eventPayload: WebhookEventPayload,
        deliveryResult: WebhookDeliveryResult,
    ) {
        runCatching {
            webhookDeliveryLogRepository.save(
                WebhookDeliveryLog.new(
                    webhookId = webhookId,
                    eventId = eventPayload.eventId,
                    eventType = eventPayload.eventType,
                    deliveryStatus = deliveryResult.status.name,
                    attemptCount = deliveryResult.attemptCount,
                    responseStatus = deliveryResult.responseStatus,
                    errorMessage = deliveryResult.errorMessage,
                ),
            )
        }.onFailure { error ->
            log.error(error) { "Failed to save webhook delivery log: webhookId=$webhookId" }
        }
    }

    private suspend fun saveDeadLetter(
        webhook: Webhook,
        webhookId: Long,
        eventPayload: WebhookEventPayload,
        deliveryResult: WebhookDeliveryResult,
    ) {
        val payloadJson =
            runCatching {
                objectMapper.writeValueAsString(eventPayload)
            }.getOrElse { error ->
                log.error(error) { "Failed to serialize webhook payload for DLQ: webhookId=$webhookId" }
                return
            }

        runCatching {
            webhookDeadLetterRepository.save(
                WebhookDeadLetter.new(
                    webhookId = webhookId,
                    eventId = eventPayload.eventId,
                    eventType = eventPayload.eventType,
                    payloadJson = payloadJson,
                    deliveryStatus = deliveryResult.status.name,
                    attemptCount = deliveryResult.attemptCount,
                    responseStatus = deliveryResult.responseStatus,
                    errorMessage = deliveryResult.errorMessage,
                ),
            )
        }.onFailure { error ->
            log.error(error) { "Failed to save webhook dead letter: webhookId=$webhookId, url=${webhook.url}" }
        }
    }
}
