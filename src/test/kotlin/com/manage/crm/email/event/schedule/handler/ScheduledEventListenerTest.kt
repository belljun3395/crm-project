package com.manage.crm.email.event.schedule.handler

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.application.service.ScheduleTaskService
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.event.schedule.CancelScheduledEvent
import com.manage.crm.infrastructure.scheduler.ScheduleName
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doNothing
import org.springframework.modulith.test.Scenario

class ScheduledEventListenerTest(
    private val scheduleTaskService: ScheduleTaskService
) : MailEventInvokeSituationTest() {
    @Test
    fun `schedule task service cancel method is called`(scenario: Scenario) {
        runTest {
            // given
            val scheduleName = ScheduleName(EventId().value)
            doNothing().`when`(awsSchedulerService).deleteSchedule(scheduleName)

            // when
            run {
                scheduleTaskService.cancel(scheduleName.value)

                val event = CancelScheduledEvent(EventId(scheduleName.value))
                `when`(cancelScheduledEventHandler.handle(event)).thenReturn(Unit)
                // then
                run {
                    scenario.publish(event)
                        .andWaitForEventOfType(CancelScheduledEvent::class.java)
                        .toArriveAndAssert { _, _ ->
                            runBlocking {
                                verify(cancelScheduledEventHandler, times(1)).handle(event)
                            }
                        }
                }
            }
        }
    }
}
