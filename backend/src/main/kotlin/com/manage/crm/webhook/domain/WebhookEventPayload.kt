package com.manage.crm.webhook.domain

data class WebhookEventPayload(
    val eventId: String,
    val eventType: String,
    val occurredAt: String,
    val data: Map<String, Any?>
)
