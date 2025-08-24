package com.manage.crm.email.controller.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class SendNotificationEmailRequest(
    val campaignId: Long?,
    @field:NotNull(message = "Template ID cannot be null")
    @field:Min(value = 1, message = "Template ID must be positive")
    val templateId: Long,
    @field:Min(value = 0, message = "Template version cannot be negative")
    val templateVersion: Float?,
    val userIds: List<Long>?
)
