package com.manage.crm.webhook.application.dto

data class BrowseWebhookDeadLettersUseCaseIn(
    val webhookId: Long,
    val limit: Int = 50
)

data class BrowseWebhookDeadLettersUseCaseOut(
    val deadLetters: List<WebhookDeadLetterDto>
)

data class WebhookDeadLetterDto(
    val id: Long,
    val webhookId: Long,
    val eventId: String,
    val eventType: String,
    val payloadJson: String,
    val deliveryStatus: String,
    val attemptCount: Int,
    val responseStatus: Int?,
    val errorMessage: String?,
    val createdAt: String?
)
