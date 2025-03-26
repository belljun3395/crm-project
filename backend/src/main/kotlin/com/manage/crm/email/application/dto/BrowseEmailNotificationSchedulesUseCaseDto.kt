package com.manage.crm.email.application.dto

import java.time.LocalDateTime

class BrowseEmailNotificationSchedulesUseCaseIn

data class BrowseEmailNotificationSchedulesUseCaseOut(
    val schedules: List<EmailNotificationScheduleDto>
)

data class EmailNotificationScheduleDto(
    val taskName: String,
    val templateId: Long,
    val userIds: List<Long>,
    val expiredTime: LocalDateTime
)
class BrowseEmailNotificationSchedulesUseCaseDto
