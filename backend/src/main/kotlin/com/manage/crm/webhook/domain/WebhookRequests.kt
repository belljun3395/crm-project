package com.manage.crm.webhook.domain

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL

data class CreateWebhookRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name must be at most 255 characters")
    val name: String,

    @field:NotBlank(message = "URL is required")
    @field:URL(message = "Invalid URL format")
    val url: String,

    @field:Size(min = 1, message = "At least one event is required")
    val events: List<String>,

    val active: Boolean? = true
)

data class UpdateWebhookRequest(
    @field:Size(max = 255, message = "Name must be at most 255 characters")
    val name: String?,

    @field:URL(message = "Invalid URL format")
    val url: String?,

    val events: List<String>?,

    val active: Boolean?
)
