package com.manage.crm.webhook.application

import com.manage.crm.support.coroutine.eventListenerCoroutineScope
import com.manage.crm.webhook.WebhookClient
import com.manage.crm.webhook.domain.WebhookEventPayload
import com.manage.crm.webhook.domain.WebhookEventType
import com.manage.crm.webhook.domain.repository.WebhookRepository
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
class WebhookDispatchService(
    private val webhookRepository: WebhookRepository,
    private val webhookClient: WebhookClient
) {
    suspend fun dispatch(eventType: WebhookEventType, payload: Map<String, Any?>) {
        val webhooks = webhookRepository.findActiveByEvent(eventType.value)
        if (webhooks.isEmpty()) {
            return
        }

        val eventPayload = WebhookEventPayload(
            eventId = UUID.randomUUID().toString(),
            eventType = eventType.value,
            occurredAt = java.time.OffsetDateTime.now().toString(),
            data = payload
        )

        eventListenerCoroutineScope().apply {
            webhooks.forEach { webhook ->
                launch {
                    webhookClient.send(webhook, eventPayload)
                }
            }
        }
    }
}
