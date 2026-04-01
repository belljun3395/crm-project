package com.manage.crm.webhook

import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEventPayload

enum class WebhookDeliveryStatus {
    SUCCESS,
    FAILED,
    BLOCKED,
}

data class WebhookDeliveryResult(
    val status: WebhookDeliveryStatus,
    val attemptCount: Int,
    val responseStatus: Int? = null,
    val errorMessage: String? = null,
)

interface WebhookClient {
    suspend fun send(
        webhook: Webhook,
        payload: WebhookEventPayload,
    ): WebhookDeliveryResult
}
