package com.manage.crm.email.application.dto

import com.fasterxml.jackson.annotation.JsonTypeName
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import java.time.LocalDateTime

data class ScheduleTaskView(
    val taskName: String,
    val campaignId: Long?,
    val templateId: Long,
    val userIds: List<Long>,
    val segmentId: Long?,
    val expiredTime: LocalDateTime
)

@JsonTypeName("notification-email-timeout")
data class NotificationEmailSendTimeOutEventInput(
    val campaignId: Long? = null,
    val templateId: Long,
    val templateVersion: Float?,
    val userIds: List<Long>,
    val segmentId: Long? = null,
    val eventId: EventId,
    val expiredTime: LocalDateTime
) : ScheduleInfo()

class ScheduleTaskServiceDto
