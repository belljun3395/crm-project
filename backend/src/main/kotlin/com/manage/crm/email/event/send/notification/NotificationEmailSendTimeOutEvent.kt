package com.manage.crm.email.event.send.notification

import com.manage.crm.email.domain.vo.EventId
import java.time.LocalDateTime

open class NotificationEmailSendTimeOutEvent(
    val campaignId: Long?,
    val eventId: EventId,
    val templateId: Long,
    val templateVersion: Float? = 1.0f,
    val userIds: List<Long>,
    val expiredTime: LocalDateTime
) {
    companion object {
        fun new(
            campaignId: Long?,
            templateId: Long,
            userIds: List<Long>,
            expiredTime: LocalDateTime
        ): NotificationEmailSendTimeOutEvent {
            if (LocalDateTime.now().isAfter(expiredTime)) {
                throw IllegalArgumentException("Expired time must be after current time. now: ${LocalDateTime.now()}, expiredTime: $expiredTime")
            }

            return NotificationEmailSendTimeOutEvent(
                campaignId = campaignId,
                eventId = EventId(),
                templateId = templateId,
                userIds = userIds,
                expiredTime = expiredTime
            )
        }
    }

    fun isExpired(time: LocalDateTime = LocalDateTime.now()): Boolean {
        return time.isAfter(expiredTime)
    }
}

// ----------------- TimeOutInvokeEvent -----------------
open class NotificationEmailSendTimeOutInvokeEvent(
    val campaignId: Long?,
    val timeOutEventId: EventId,
    val templateId: Long,
    val templateVersion: Float?,
    val userIds: List<Long>
)
