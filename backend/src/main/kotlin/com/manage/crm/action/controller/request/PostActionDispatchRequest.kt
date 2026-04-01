package com.manage.crm.action.controller.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PostActionDispatchRequest(
    @field:NotBlank(message = "Channel is required")
    val channel: String,
    @field:NotBlank(message = "Destination is required")
    @field:Size(max = 1024, message = "Destination must be at most 1024 characters")
    val destination: String,
    @field:Size(max = 255, message = "Subject must be at most 255 characters")
    val subject: String? = null,
    @field:NotBlank(message = "Body is required")
    val body: String,
    val variables: Map<String, String>? = emptyMap(),
    val campaignId: Long? = null,
    val journeyExecutionId: Long? = null,
)
