package com.manage.crm.webhook.application.dto

class BrowseWebhookUseCaseIn

data class BrowseWebhookUseCaseOut(
    val webhooks: List<WebhookDto>
)

data class WebhookDto(
    val id: Long,
    val name: String,
    val url: String,
    val events: List<String>,
    val active: Boolean,
    val createdAt: String?
)

class BrowseWebhookUseCaseDto
