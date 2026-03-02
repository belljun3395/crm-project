package com.manage.crm.email.controller.request

import java.time.LocalDateTime

data class PostNotificationEmailRequest(
    val campaignId: Long? = null,
    val templateId: Long,
    val templateVersion: Float?,
    val userIds: List<Long>? = null,
    val segmentId: Long? = null,
    val expiredTime: LocalDateTime
)
