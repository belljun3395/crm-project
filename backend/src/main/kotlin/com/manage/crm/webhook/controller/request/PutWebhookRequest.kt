package com.manage.crm.webhook.controller.request

import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL

data class PutWebhookRequest(
    @field:Size(max = 255, message = "Name must be at most 255 characters")
    val name: String?,

    @field:URL(message = "Invalid URL format")
    val url: String?,

    val events: List<String>?,

    val active: Boolean?
)
