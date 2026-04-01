package com.manage.crm.webhook.application.dto

data class BrowseWebhookDeliveryLogsUseCaseIn(
    val webhookId: Long,
    val limit: Int = 50,
)

data class BrowseWebhookDeliveryLogsUseCaseOut(
    val deliveries: List<WebhookDeliveryLogDto>,
)

data class WebhookDeliveryLogDto(
    val id: Long,
    val webhookId: Long,
    val eventId: String,
    val eventType: String,
    val deliveryStatus: String,
    val attemptCount: Int,
    val responseStatus: Int?,
    val errorMessage: String?,
    val deliveredAt: String?,
)
