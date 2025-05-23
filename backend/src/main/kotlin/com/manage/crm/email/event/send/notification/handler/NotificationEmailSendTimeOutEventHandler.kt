package com.manage.crm.email.event.send.notification.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.domain.ScheduledEvent
import com.manage.crm.email.domain.repository.ScheduledEventRepository
import com.manage.crm.email.domain.vo.ScheduleType
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutEvent
import org.springframework.stereotype.Component

@Component
class NotificationEmailSendTimeOutEventHandler(
    private val scheduledEventRepository: ScheduledEventRepository,
    private val objectMapper: ObjectMapper
) {
    /**
     * - Save Scheduled Event
     */
    suspend fun handle(event: NotificationEmailSendTimeOutEvent) {
        val scheduledAt = ScheduleType.AWS.name

        scheduledEventRepository.save(
            ScheduledEvent.new(
                eventId = event.eventId,
                eventClass = event.javaClass.simpleName,
                eventPayload = objectMapper.writeValueAsString(event),
                completed = false,
                scheduledAt = scheduledAt
            )
        )
    }
}
