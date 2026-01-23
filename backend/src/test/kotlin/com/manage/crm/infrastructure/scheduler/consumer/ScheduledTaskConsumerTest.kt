package com.manage.crm.infrastructure.scheduler.consumer

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDateTime

class ScheduledTaskConsumerTest {

    private val publisher: ApplicationEventPublisher = mockk(relaxed = true)
    private val acknowledgment: Acknowledgment = mockk {
        every { acknowledge() } just runs
    }
    private val consumer = ScheduledTaskConsumer(publisher)

    @Test
    fun `publishes notification timeout event and acknowledges message`() {
        val payload = NotificationEmailSendTimeOutEventInput(
            templateId = 1L,
            templateVersion = 1.0f,
            userIds = listOf(1L, 2L),
            eventId = EventId("event-1"),
            expiredTime = LocalDateTime.now()
        )
        val event = ScheduledTaskEvent(
            scheduleName = "event-1",
            scheduleTime = payload.expiredTime,
            payload = payload
        )

        consumer.consume(event, acknowledgment)

        verify(exactly = 1) {
            publisher.publishEvent(
                match<NotificationEmailSendTimeOutInvokeEvent> { it.timeOutEventId == payload.eventId }
            )
        }
        verify(exactly = 1) { acknowledgment.acknowledge() }
    }
}
