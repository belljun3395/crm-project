package com.manage.crm.email.event.send.notification

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.service.ScheduleTaskServicePostEventProcessor
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.event.relay.aws.ScheduledEventReverseRelay
import com.manage.crm.email.event.relay.aws.mapper.ScheduledEventMessageMapper
import com.manage.crm.email.support.EmailEventPublisher
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.modulith.test.Scenario
import software.amazon.awssdk.services.scheduler.model.CreateScheduleResponse
import java.time.LocalDateTime
import kotlin.test.assertEquals

class NotificationEmailSendTimeOutEventListenerTest(
    private val scheduleTaskService: ScheduleTaskServicePostEventProcessor,
    scheduledEventMessageMapper: ScheduledEventMessageMapper
) : MailEventInvokeSituationTest() {

    private val scheduledEventReverseRelayEmailEventPublisher = mock(EmailEventPublisher::class.java)
    private var scheduledEventReverseRelay =
        ScheduledEventReverseRelay(
            scheduledEventReverseRelayEmailEventPublisher,
            scheduledEventMessageMapper
        )

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

            val event = NotificationEmailSendTimeOutEvent(
                eventId = EventId("1"),
                templateId = 1,
                templateVersion = 1.0f,
                userIds = listOf(1L),
                expiredTime = expiredTime
            )
            doNothing().`when`(emailEventPublisher).publishEvent(event)

            // when
            scheduleTaskService.newSchedule(input)

            `when`(notificationEmailSendTimeOutEventHandler.handle(event)).thenReturn(Unit)

            // then
            val expectedInvocationTime = 1
            scenario.publish(event)
                .andWaitForStateChange(
                    { mockingDetails(notificationEmailSendTimeOutEventHandler).invocations.size },
                    { mockingDetails(notificationEmailSendTimeOutEventHandler).invocations.size == expectedInvocationTime }
                )
                .andVerify { invocationTime ->
                    assertEquals(invocationTime, expectedInvocationTime)
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
            val acknowledgement = mock(Acknowledgement::class.java)
            doNothing().`when`(acknowledgement).acknowledge()

            val event = NotificationEmailSendTimeOutInvokeEvent(
                timeOutEventId = EventId("1"),
                templateId = 1,
                templateVersion = 1.0f,
                userIds = listOf(1L)
            )
            doNothing().`when`(scheduledEventReverseRelayEmailEventPublisher).publishEvent(event)

            // when
            scheduledEventReverseRelay.onMessage(message, acknowledgement)

            `when`(notificationEmailSendTimeOutInvokeEventHandler.handle(event)).thenReturn(Unit)

            // then
            val expectedInvocationTime = 1
            scenario.publish(event)
                .andWaitForStateChange(
                    { mockingDetails(notificationEmailSendTimeOutInvokeEventHandler).invocations.size },
                    { mockingDetails(notificationEmailSendTimeOutInvokeEventHandler).invocations.size == expectedInvocationTime }
                )
                .andVerify { invocationTime ->
                    assertEquals(invocationTime, expectedInvocationTime)
                }
        }
    }
}
