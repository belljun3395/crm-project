package com.manage.crm.email.application.dto

import java.time.LocalDateTime

class PostEmailNotificationSchedulesUseCaseIn(
    val campaignId: Long?,
    val templateId: Long,
    val templateVersion: Float? = 1.0f,
    val userIds: List<Long>,
    val expiredTime: LocalDateTime
)

class PostEmailNotificationSchedulesUseCaseOut(
    val newSchedule: String
)

class PostEmailNotificationSchedulesUseCaseDto
