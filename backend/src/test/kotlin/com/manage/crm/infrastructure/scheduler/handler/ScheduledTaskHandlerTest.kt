package com.manage.crm.infrastructure.scheduler.handler

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime

class ScheduledTaskHandlerTest {
    private val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val handler = ScheduledTaskHandler(applicationEventPublisher)

    @Test
    fun `should publish NotificationEmailSendTimeOutInvokeEvent when payload is NotificationEmailSendTimeOutEventInput`() {
        // Given
        val input = NotificationEmailSendTimeOutEventInput(
            templateId = 1L,
            templateVersion = 1.0f,
            userIds = listOf(1L, 2L),
            eventId = EventId("test-event"),
            expiredTime = LocalDateTime.now().plusHours(1)
        )

        // When
        handler.handle(input)

        // Then
        verify(exactly = 1) {
            applicationEventPublisher.publishEvent(match<NotificationEmailSendTimeOutInvokeEvent> {
                it.timeOutEventId == input.eventId &&
                    it.templateId == input.templateId &&
                    it.templateVersion == input.templateVersion &&
                    it.userIds == input.userIds
            })
        }
        confirmVerified(applicationEventPublisher)
    }
}
