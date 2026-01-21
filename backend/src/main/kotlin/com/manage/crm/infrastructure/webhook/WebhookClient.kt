package com.manage.crm.infrastructure.webhook

import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEventPayload

interface WebhookClient {
    suspend fun send(webhook: Webhook, payload: WebhookEventPayload)
}
