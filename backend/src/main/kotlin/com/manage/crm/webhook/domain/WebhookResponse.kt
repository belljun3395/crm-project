package com.manage.crm.webhook.domain

data class WebhookResponse(
    val id: Long,
    val name: String,
    val url: String,
    val events: List<String>,
    val active: Boolean,
    val createdAt: String?
)
