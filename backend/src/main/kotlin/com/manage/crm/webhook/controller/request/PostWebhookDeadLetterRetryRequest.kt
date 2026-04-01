package com.manage.crm.webhook.controller.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class PostWebhookDeadLetterRetryRequest(
    val deadLetterIds: List<Long> = emptyList(),
    @field:Min(1)
    @field:Max(200)
    val limit: Int = 50,
)
