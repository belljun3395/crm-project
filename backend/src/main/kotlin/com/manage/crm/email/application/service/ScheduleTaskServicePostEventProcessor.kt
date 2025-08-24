package com.manage.crm.email.application.service

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.dto.ScheduleTaskView
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.event.schedule.CancelScheduledEvent
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutEvent
import com.manage.crm.email.support.EmailEventPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ScheduleTaskServicePostEventProcessor(
    @Qualifier("scheduleTaskServiceImpl")
    private val scheduleTaskService: ScheduleTaskAllService,
    private val emailEventPublisher: EmailEventPublisher
) : ScheduleTaskAllService {
    val log = KotlinLogging.logger {}

    /**
     * 새로운 스케줄을 등록하고 이벤트 관련 후처리를 수행합니다.
     */
    override fun newSchedule(input: NotificationEmailSendTimeOutEventInput): String {
        return newScheduleEventProcess(scheduleTaskService.newSchedule(input), input)
    }

    /**
     * 등록한 스케줄을 취소하고 이벤트 관련 후처리를 수행합니다.
     */
    override fun cancel(scheduleName: String) {
        scheduleTaskService.cancel(scheduleName).let {
            cancelEventProcess(scheduleName)
        }
    }

    /**
     * 등록한 스케줄을 재등록하고 이벤트 관련 후처리를 수행합니다.
     */
    override fun reSchedule(input: NotificationEmailSendTimeOutEventInput) {
        scheduleTaskService.reSchedule(input).let {
            reScheduleEventProcess(input)
        }
    }

    /**
     * 새로운 스케줄 등록 이벤트를 발행합니다.
     */
    fun newScheduleEventProcess(
        result: String,
        input: NotificationEmailSendTimeOutEventInput
    ): String {
        emailEventPublisher.publishEvent(
            NotificationEmailSendTimeOutEvent(
                campaignId = input.campaignId,
                eventId = input.eventId,
                templateId = input.templateId,
                templateVersion = input.templateVersion,
                userIds = input.userIds,
                expiredTime = input.expiredTime
            )
        )
        return result
    }

    /**
     * 등록한 스케줄을 취소하는 이벤트를 발행합니다.
     */
    fun cancelEventProcess(scheduleName: String) {
        val cancelScheduledEvent = CancelScheduledEvent(
            scheduledEventId = EventId(scheduleName)
        )
        emailEventPublisher.publishEvent(cancelScheduledEvent)
    }

    /**
     * `newScheduleEventProcess`와 `cancelEventProcess`를 호출하여 스케줄을 재등록합니다.
     */
    fun reScheduleEventProcess(input: NotificationEmailSendTimeOutEventInput) {
        cancelEventProcess(input.eventId.value)
        newScheduleEventProcess(input.eventId.value, input)
    }

    override suspend fun browseScheduledTasksView(): List<ScheduleTaskView> {
        return scheduleTaskService.browseScheduledTasksView()
    }
}
