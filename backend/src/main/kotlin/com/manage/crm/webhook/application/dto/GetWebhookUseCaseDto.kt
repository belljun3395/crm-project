package com.manage.crm.webhook.application.dto

data class GetWebhookUseCaseIn(
    val id: Long
)

data class GetWebhookUseCaseOut(
    val webhook: WebhookDto
)

class GetWebhookUseCaseDto
