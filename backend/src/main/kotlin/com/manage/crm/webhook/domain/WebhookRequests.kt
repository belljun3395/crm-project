package com.manage.crm.webhook.domain

data class CreateWebhookRequest(
    val name: String,
    val url: String,
    val events: List<String>,
    val active: Boolean? = true
)

data class UpdateWebhookRequest(
    val name: String?,
    val url: String?,
    val events: List<String>?,
    val active: Boolean?
)
