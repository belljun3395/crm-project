package com.manage.crm.email.controller.request

import java.time.LocalDateTime

data class PostNotificationEmailRequest(
    val campaignId: Long?,
    val templateId: Long,
    val templateVersion: Float?,
    val userIds: List<Long>,
    val expiredTime: LocalDateTime
)
