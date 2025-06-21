package com.manage.crm.email.event.send.notification

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.service.ScheduleTaskAllService
import com.manage.crm.email.domain.EmailTemplateFixtures
import com.manage.crm.email.domain.vo.EventIdFixtures
import com.manage.crm.email.event.relay.aws.ScheduledEventReverseRelay
import com.manage.crm.email.event.relay.aws.mapper.ScheduledEventMessageMapper
import com.manage.crm.email.support.EmailEventPublisher
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.modulith.test.Scenario
import software.amazon.awssdk.services.scheduler.model.CreateScheduleResponse
import java.time.LocalDateTime
import kotlin.test.assertEquals

class NotificationEmailSendTimeOutEventListenerTest(
    @Qualifier("scheduleTaskServicePostEventProcessor")
    private val scheduleTaskService: ScheduleTaskAllService,
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
            val template = EmailTemplateFixtures.giveMeOne().build()
            val eventId = EventIdFixtures.giveMeOne().build()
            val expiredTime = LocalDateTime.now().plusNanos(1)
            val userIds = listOf(1L)
            val input = NotificationEmailSendTimeOutEventInput(
                templateId = template.id!!,
                templateVersion = template.version.value,
                userIds = userIds,
                eventId = eventId,
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
                eventId = eventId,
                templateId = template.id!!,
                templateVersion = template.version.value,
                userIds = userIds,
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

            val template = EmailTemplateFixtures.giveMeOne().build()
            val eventId = EventIdFixtures.giveMeOne().build()
            val userIds = listOf(1L)
            val event = NotificationEmailSendTimeOutInvokeEvent(
                timeOutEventId = eventId,
                templateId = template.id!!,
                templateVersion = template.version.value,
                userIds = userIds
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
