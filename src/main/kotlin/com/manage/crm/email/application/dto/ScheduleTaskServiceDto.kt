package com.manage.crm.email.application.dto

import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import java.time.LocalDateTime

data class ScheduleTaskView(
    val taskName: String,
    val templateId: Long,
    val userIds: List<Long>,
    val expiredTime: LocalDateTime
)

data class NotificationEmailSendTimeOutEventInput(
    val templateId: Long,
    val templateVersion: Float?,
    val userIds: List<Long>,
    val eventId: EventId,
    val expiredTime: LocalDateTime
) : ScheduleInfo()

class ScheduleTaskServiceDto
