package com.manage.crm.email.event.send.notification

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.service.ScheduleTaskAllService
import com.manage.crm.email.domain.EmailTemplateFixtures
import com.manage.crm.email.domain.vo.EventIdFixtures
import com.manage.crm.email.support.EmailEventPublisher
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Qualifier
import java.time.LocalDateTime

class NotificationEmailSendTimeOutEventListenerTest(
    @Qualifier("scheduleTaskServicePostEventProcessor")
    private val scheduleTaskService: ScheduleTaskAllService,
) : MailEventInvokeSituationTest() {
    private val mockPublisher = mock(EmailEventPublisher::class.java)

    init {
        given("schedule task service") {
            then("schedule task service new schedule method is called") {
                val template = EmailTemplateFixtures.giveMeOne().build()
                val eventId = EventIdFixtures.giveMeOne().build()
                val expiredTime = LocalDateTime.now().plusNanos(1)
                val userIds = listOf(1L)
                val input =
                    NotificationEmailSendTimeOutEventInput(
                        templateId = template.id!!,
                        templateVersion = template.version.value,
                        userIds = userIds,
                        eventId = eventId,
                        expiredTime = expiredTime,
                    )

                val event =
                    NotificationEmailSendTimeOutEvent(
                        eventId = eventId,
                        templateId = template.id!!,
                        templateVersion = template.version.value,
                        userIds = userIds,
                        expiredTime = expiredTime,
                    )
                doNothing().`when`(emailEventPublisher).publishEvent(event)

                scheduleTaskService.newSchedule(input)

                verify(emailEventPublisher, times(1)).publishEvent(
                    argThat<NotificationEmailSendTimeOutEvent> {
                        eventId == event.eventId &&
                            templateId == event.templateId &&
                            templateVersion == event.templateVersion &&
                            userIds == event.userIds
                    },
                )
            }
        }

        given("scheduled task consumer") {
            then("consume notification email timeout invoke event") {
                val template = EmailTemplateFixtures.giveMeOne().build()
                val eventId = EventIdFixtures.giveMeOne().build()
                val userIds = listOf(1L)
                val event =
                    NotificationEmailSendTimeOutInvokeEvent(
                        timeOutEventId = eventId,
                        templateId = template.id!!,
                        templateVersion = template.version.value,
                        userIds = userIds,
                    )
                doNothing().`when`(mockPublisher).publishEvent(event)
                mockPublisher.publishEvent(event)

                verify(mockPublisher, times(1)).publishEvent(any<NotificationEmailSendTimeOutInvokeEvent>())
            }
        }
    }
}
