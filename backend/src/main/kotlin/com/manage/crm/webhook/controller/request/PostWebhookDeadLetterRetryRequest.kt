package com.manage.crm.webhook.controller.request

data class PostWebhookDeadLetterRetryRequest(
    val deadLetterIds: List<Long>? = emptyList(),
    val limit: Int? = 50
)
