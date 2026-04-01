package com.manage.crm.webhook.application.dto

data class RetryWebhookDeadLettersUseCaseIn(
    val webhookId: Long,
    val deadLetterIds: List<Long> = emptyList(),
    val limit: Int = 50,
)

data class RetryWebhookDeadLettersUseCaseOut(
    val results: List<WebhookDeadLetterRetryResultDto>,
)

data class WebhookDeadLetterRetryResultDto(
    val deadLetterId: Long,
    val webhookId: Long,
    val eventId: String,
    val status: String,
    val attemptCount: Int,
    val responseStatus: Int?,
    val errorMessage: String?,
)
