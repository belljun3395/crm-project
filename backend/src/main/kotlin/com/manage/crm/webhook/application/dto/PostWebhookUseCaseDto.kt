package com.manage.crm.webhook.application.dto

data class PostWebhookUseCaseIn(
    val id: Long? = null,
    val name: String,
    val url: String,
    val events: List<String>,
    val active: Boolean? = true
)

data class PostWebhookUseCaseOut(
    val id: Long,
    val name: String,
    val url: String,
    val events: List<String>,
    val active: Boolean,
    val createdAt: String?
)

class PostWebhookUseCaseDto
