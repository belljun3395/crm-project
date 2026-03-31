package com.manage.crm.email.event.schedule.handler

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.application.service.ScheduleTaskServicePostEventProcessor
import com.manage.crm.email.domain.vo.EventIdFixtures
import com.manage.crm.email.event.schedule.CancelScheduledEvent
import com.manage.crm.infrastructure.scheduler.ScheduleName
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class ScheduledEventListenerTest(
    private val scheduleTaskService: ScheduleTaskServicePostEventProcessor
) : MailEventInvokeSituationTest() {
    init {
        given("schedule task service") {
            then("cancel method is called") {
                val scheduleName = ScheduleName(EventIdFixtures.giveMeOne().build().value)
                doNothing().`when`(awsSchedulerService).deleteSchedule(scheduleName)

                val event = CancelScheduledEvent(EventIdFixtures.giveMeOne().withValue(scheduleName.value).build())
                doNothing().`when`(emailEventPublisher).publishEvent(event)

                scheduleTaskService.cancel(scheduleName.value)

                verify(emailEventPublisher, times(1)).publishEvent(
                    argThat<CancelScheduledEvent> { scheduledEventId == event.scheduledEventId }
                )
            }
        }
    }
}
