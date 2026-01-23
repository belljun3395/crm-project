package com.manage.crm.infrastructure.scheduler.consumer

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import com.manage.crm.infrastructure.scheduler.handler.ScheduledTaskHandler
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDateTime

class ScheduledTaskConsumerTest {

    private val handler: ScheduledTaskHandler = mockk(relaxed = true)
    private val acknowledgment: Acknowledgment = mockk {
        every { acknowledge() } just runs
    }
    private val consumer = ScheduledTaskConsumer(handler)

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
            handler.handle(payload)
        }
        verify(exactly = 1) { acknowledgment.acknowledge() }
    }
}
