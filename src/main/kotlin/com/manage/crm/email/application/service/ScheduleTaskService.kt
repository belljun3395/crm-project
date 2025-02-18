package com.manage.crm.email.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.dto.ScheduleTaskView
import com.manage.crm.email.domain.repository.ScheduledEventRepository
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.event.schedule.CancelScheduledEvent
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutEvent
import com.manage.crm.infrastructure.scheduler.ScheduleName
import com.manage.crm.infrastructure.scheduler.provider.AwsSchedulerService
import com.manage.crm.support.parseISOExpiredTime
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ScheduleTaskService(
    private val scheduledEventRepository: ScheduledEventRepository,
    private val awsSchedulerService: AwsSchedulerService,
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    val log = KotlinLogging.logger {}

    fun newSchedule(input: NotificationEmailSendTimeOutEventInput): String {
        val enrolledSchedule = awsSchedulerService.createSchedule(
            name = input.eventId.value,
            schedule = input.expiredTime,
            input = input
        )

        applicationEventPublisher.publishEvent(
            NotificationEmailSendTimeOutEvent(
                eventId = input.eventId,
                templateId = input.templateId,
                templateVersion = input.templateVersion,
                userIds = input.userIds,
                expiredTime = input.expiredTime,
                eventPublisher = applicationEventPublisher
            )
        )
        return enrolledSchedule.scheduleArn()
    }

    fun cancel(scheduleName: String) {
        awsSchedulerService.deleteSchedule(ScheduleName(scheduleName))
        applicationEventPublisher.publishEvent(
            CancelScheduledEvent(
                scheduledEventId = EventId(scheduleName)
            )
        )
        log.info { "Task $scheduleName is cancelled" }
    }

    fun reSchedule(input: NotificationEmailSendTimeOutEventInput) {
        cancel(input.eventId.value)
        newSchedule(input)
    }

    suspend fun browseScheduledTasksView(): List<ScheduleTaskView> {
        val awsScheduleViews = awsSchedulerService.browseSchedule()
            .map { EventId(it.value) }
            .filter { it.value.matches(Regex("[a-f0-9]{8}-([a-f0-9]{4}-){3}[a-f0-9]{12}")) }

        return scheduledEventRepository
            .findAllByEventIdIn(awsScheduleViews)
            .map {
                // TODO: refactor readValue to readTree
                val payload = objectMapper.readValue(it.eventPayload, Map::class.java).toMutableMap()
                payload["eventId"] = it.eventId?.value
                payload
            }.map { payload ->
                ScheduleTaskView(
                    taskName = payload["eventId"] as String,
                    templateId = (payload["templateId"] as Int).toLong(),
                    userIds = (payload["userIds"] as List<Int>).map { it.toLong() },
                    expiredTime = (payload["expiredTime"] as String).parseISOExpiredTime()
                )
            }.toList()
    }
}
