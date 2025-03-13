package com.manage.crm.email.event.schedule.handler

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.application.service.ScheduleTaskServicePostEventProcessor
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.event.schedule.CancelScheduledEvent
import com.manage.crm.infrastructure.scheduler.ScheduleName
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doNothing
import org.springframework.modulith.test.Scenario
import kotlin.test.assertEquals

class ScheduledEventListenerTest(
    private val scheduleTaskService: ScheduleTaskServicePostEventProcessor
) : MailEventInvokeSituationTest() {
    @Test
    fun `schedule task service cancel method is called`(scenario: Scenario) {
        runTest {
            // given
            val scheduleName = ScheduleName(EventId().value)
            doNothing().`when`(awsSchedulerService).deleteSchedule(scheduleName)

            val event = CancelScheduledEvent(EventId(scheduleName.value))
            doNothing().`when`(emailEventPublisher).publishEvent(event)

            // when
            scheduleTaskService.cancel(scheduleName.value)

            `when`(cancelScheduledEventHandler.handle(event)).thenReturn(Unit)

            // then
            val expectedInvocationTime = 1
            scenario.publish(event)
                .andWaitForStateChange(
                    { mockingDetails(cancelScheduledEventHandler).invocations.size },
                    { mockingDetails(cancelScheduledEventHandler).invocations.size == expectedInvocationTime }
                )
                .andVerify { invocationTime ->
                    assertEquals(invocationTime, expectedInvocationTime)
                }
        }
    }
}
