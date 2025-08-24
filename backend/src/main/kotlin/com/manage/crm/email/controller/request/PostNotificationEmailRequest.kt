package com.manage.crm.email.controller.request

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class PostNotificationEmailRequest(
    val campaignId: Long?,
    @field:NotNull(message = "Template ID cannot be null")
    @field:Min(value = 1, message = "Template ID must be positive")
    val templateId: Long,
    @field:Min(value = 0, message = "Template version cannot be negative")
    val templateVersion: Float?,
    @field:NotEmpty(message = "User IDs list cannot be empty")
    val userIds: List<Long>,
    @field:NotNull(message = "Expired time cannot be null")
    @field:Future(message = "Expired time must be in the future")
    val expiredTime: LocalDateTime
)
