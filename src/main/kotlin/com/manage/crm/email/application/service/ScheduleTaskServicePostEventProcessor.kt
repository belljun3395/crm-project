package com.manage.crm.email.application.service

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.event.schedule.CancelScheduledEvent
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutEvent
import com.manage.crm.email.support.EmailEventPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class ScheduleTaskServicePostEventProcessor(
    private val scheduleTaskServiceImpl: ScheduleTaskServiceImpl,
    private val emailEventPublisher: EmailEventPublisher
) : ScheduleTaskService {
    val log = KotlinLogging.logger {}

    override fun newSchedule(input: NotificationEmailSendTimeOutEventInput): String {
        return newScheduleEventProcess(scheduleTaskServiceImpl.newSchedule(input), input)
    }

    override fun cancel(scheduleName: String) {
        scheduleTaskServiceImpl.cancel(scheduleName).let {
            cancelEventProcess(scheduleName)
        }
    }

    override fun reSchedule(input: NotificationEmailSendTimeOutEventInput) {
        scheduleTaskServiceImpl.reSchedule(input).let {
            reScheduleEventProcess(input)
        }
    }

    fun newScheduleEventProcess(
        result: String,
        input: NotificationEmailSendTimeOutEventInput
    ): String {
        emailEventPublisher.publishEvent(
            NotificationEmailSendTimeOutEvent(
                eventId = input.eventId,
                templateId = input.templateId,
                templateVersion = input.templateVersion,
                userIds = input.userIds,
                expiredTime = input.expiredTime
            )
        )
        return result
    }

    fun cancelEventProcess(scheduleName: String) {
        val cancelScheduledEvent = CancelScheduledEvent(
            scheduledEventId = EventId(scheduleName)
        )
        emailEventPublisher.publishEvent(cancelScheduledEvent)
    }

    fun reScheduleEventProcess(input: NotificationEmailSendTimeOutEventInput) {
        cancelEventProcess(input.eventId.value)
        newScheduleEventProcess(input.eventId.value, input)
    }
}
