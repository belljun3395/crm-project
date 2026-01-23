package com.manage.crm.webhook.application.dto

data class DeleteWebhookUseCaseIn(
    val id: Long
)

data class DeleteWebhookUseCaseOut(
    val success: Boolean
)
