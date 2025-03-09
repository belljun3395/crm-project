package com.manage.crm.email.event.send.notification

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.service.ScheduleTaskService
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.event.relay.aws.ScheduledEventReverseRelay
import com.manage.crm.email.event.relay.aws.mapper.ScheduledEventMessageMapper
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.modulith.test.Scenario
import software.amazon.awssdk.services.scheduler.model.CreateScheduleResponse
import java.time.LocalDateTime

class NotificationEmailSendTimeOutEventListenerTest(
    private val scheduleTaskService: ScheduleTaskService,
    private val eventPublisher: ApplicationEventPublisher,
    scheduledEventMessageMapper: ScheduledEventMessageMapper
) : MailEventInvokeSituationTest() {

    private var scheduledEventReverseRelay =
        ScheduledEventReverseRelay(eventPublisher, scheduledEventMessageMapper)

    @Test
    fun `schedule task service new schedule method is called`(scenario: Scenario) {
        runTest {
            // given
            val expiredTime = LocalDateTime.now().plusNanos(1)
            val input = NotificationEmailSendTimeOutEventInput(
                templateId = 1,
                templateVersion = 1.0f,
                userIds = listOf(1L),
                eventId = EventId("1"),
                expiredTime = expiredTime
            )
            `when`(
                awsSchedulerService.createSchedule(
                    name = input.eventId.value,
                    schedule = input.expiredTime,
                    input = input
                )
            ).thenReturn(CreateScheduleResponse.builder().scheduleArn("arn").build())

            // when
            scheduleTaskService.newSchedule(input)

            val event = NotificationEmailSendTimeOutEvent(
                eventId = EventId("1"),
                templateId = 1,
                templateVersion = 1.0f,
                userIds = listOf(1L),
                expiredTime = expiredTime
            )
            `when`(notificationEmailSendTimeOutEventHandler.handle(event)).thenReturn(Unit)

            // then
            scenario.publish(event)
                .andWaitForEventOfType(NotificationEmailSendTimeOutEvent::class.java)
                .toArriveAndAssert { _, _ ->
                    runBlocking {
                        verify(notificationEmailSendTimeOutEventHandler, times(1)).handle(
                            event
                        )
                    }
                }
        }
    }

    @Test
    fun `scheduled notification email event from aws scheduler`(scenario: Scenario) {
        runTest {
            // given
            val message = """
                    {
                        "templateId": 1,
                        "templateVersion": 1.0,
                        "userIds": [1],
                        "eventId": "1"
                    }
            """.trimIndent()
            val acknowledgement = Mockito.mock(Acknowledgement::class.java)
            doNothing().`when`(acknowledgement).acknowledge()

            // when
            scheduledEventReverseRelay.onMessage(message, acknowledgement)
            val event = NotificationEmailSendTimeOutInvokeEvent(
                timeOutEventId = EventId("1"),
                templateId = 1,
                templateVersion = 1.0f,
                userIds = listOf(1L)
            )

            // then
            scenario.publish(event)
                .andWaitForEventOfType(NotificationEmailSendTimeOutInvokeEvent::class.java)
                .toArriveAndAssert { _, _ ->
                    runBlocking {
                        verify(notificationEmailSendTimeOutInvokeEventHandler, times(1)).handle(event)
                    }
                }
        }
    }
}
