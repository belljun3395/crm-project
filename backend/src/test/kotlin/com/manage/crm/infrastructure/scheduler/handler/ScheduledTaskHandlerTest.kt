package com.manage.crm.infrastructure.scheduler.handler

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import io.kotest.core.spec.style.FeatureSpec
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime

class ScheduledTaskHandlerTest : FeatureSpec({
    lateinit var eventPublisher: ApplicationEventPublisher
    lateinit var handler: ScheduledTaskHandler

    beforeTest {
        eventPublisher = mockk(relaxed = true)
        handler = ScheduledTaskHandler(eventPublisher)
    }

    feature("ScheduledTaskHandler#handle") {
        scenario("should process NotificationEmailSendTimeOutEventInput and publish invoke event") {
            // given
            val eventId = EventId("test-event-123")
            val templateId = 1L
            val templateVersion = 1.0f
            val userIds = listOf(1L, 2L, 3L)
            val expiredTime = LocalDateTime.now()

            val input = NotificationEmailSendTimeOutEventInput(
                templateId = templateId,
                templateVersion = templateVersion,
                userIds = userIds,
                eventId = eventId,
                expiredTime = expiredTime
            )

            val event = ScheduledTaskEvent(
                scheduleName = eventId.value,
                scheduleTime = expiredTime,
                payload = input
            )

            val capturedEvent = slot<NotificationEmailSendTimeOutInvokeEvent>()

            // when
            handler.handle(event)

            // then
            verify(exactly = 1) {
                eventPublisher.publishEvent(capture(capturedEvent))
            }

            val publishedEvent = capturedEvent.captured
            publishedEvent.timeOutEventId.value == eventId.value
            publishedEvent.templateId == templateId
            publishedEvent.templateVersion == templateVersion
            publishedEvent.userIds == userIds
        }
    }
})
