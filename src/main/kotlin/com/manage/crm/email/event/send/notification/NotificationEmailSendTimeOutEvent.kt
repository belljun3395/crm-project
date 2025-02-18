package com.manage.crm.email.event.send.notification

import com.fasterxml.jackson.annotation.JsonIgnore
import com.manage.crm.email.domain.vo.EventId
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime

open class NotificationEmailSendTimeOutEvent(
    val eventId: EventId,
    val templateId: Long,
    val templateVersion: Float? = 1.0f,
    val userIds: List<Long>,
    val expiredTime: LocalDateTime,
    var completed: Boolean = false,
    @JsonIgnore
    val eventPublisher: ApplicationEventPublisher
) : Runnable {
    companion object {
        fun new(
            templateId: Long,
            userIds: List<Long>,
            expiredTime: LocalDateTime,
            eventPublisher: ApplicationEventPublisher
        ): NotificationEmailSendTimeOutEvent {
            if (LocalDateTime.now().isAfter(expiredTime)) {
                throw IllegalArgumentException("Expired time must be after current time. now: ${LocalDateTime.now()}, expiredTime: $expiredTime")
            }

            return NotificationEmailSendTimeOutEvent(
                eventId = EventId(),
                templateId = templateId,
                userIds = userIds,
                expiredTime = expiredTime,
                eventPublisher = eventPublisher
            )
        }
    }

    override fun run() {
        eventPublisher.publishEvent(
            NotificationEmailSendTimeOutInvokeEvent(
                timeOutEventId = eventId,
                templateId = templateId,
                templateVersion = templateVersion,
                userIds = userIds
            )
        )
    }

    fun complete() {
        completed = true
    }

    fun isExpired(time: LocalDateTime = LocalDateTime.now()): Boolean {
        return !completed && time.isAfter(expiredTime)
    }

    fun isLongTermEvent(now: LocalDateTime): Boolean {
        return expiredTime.isAfter(now)
    }
}

// ----------------- TimeOutInvokeEvent -----------------
open class NotificationEmailSendTimeOutInvokeEvent(
    val timeOutEventId: EventId,
    val templateId: Long,
    val templateVersion: Float?,
    val userIds: List<Long>
)
