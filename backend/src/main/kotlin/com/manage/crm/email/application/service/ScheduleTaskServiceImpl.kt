package com.manage.crm.email.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.dto.ScheduleTaskView
import com.manage.crm.email.domain.repository.ScheduledEventRepository
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.ScheduleName
import com.manage.crm.infrastructure.scheduler.provider.AwsSchedulerService
import com.manage.crm.support.asLong
import com.manage.crm.support.parseISOExpiredTime
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class ScheduleTaskServiceImpl(
    private val scheduledEventRepository: ScheduledEventRepository,
    private val awsSchedulerService: AwsSchedulerService,
    private val objectMapper: ObjectMapper
) : ScheduleTaskAllService {
    val log = KotlinLogging.logger {}

    override fun newSchedule(input: NotificationEmailSendTimeOutEventInput): String {
        awsSchedulerService.createSchedule(
            name = input.eventId.value,
            schedule = input.expiredTime,
            input = input
        )
        return input.eventId.value
    }

    override fun cancel(scheduleName: String) {
        awsSchedulerService.deleteSchedule(ScheduleName(scheduleName))
        log.info { "Task $scheduleName is cancelled" }
    }

    override fun reSchedule(input: NotificationEmailSendTimeOutEventInput) {
        cancel(input.eventId.value)
        newSchedule(input)
    }

    override suspend fun browseScheduledTasksView(): List<ScheduleTaskView> {
        val awsScheduleViews = awsSchedulerService.browseSchedule()
            .map { EventId(it.value) }
            .filter { it.value.matches(Regex("[a-f0-9]{8}-([a-f0-9]{4}-){3}[a-f0-9]{12}")) }

        return scheduledEventRepository
            .findAllByEventIdIn(awsScheduleViews)
            .map {
                // TODO: refactor readValue to readTree
                val payload = objectMapper.readValue(it.eventPayload, Map::class.java).toMutableMap()
                payload["eventId"] = it.eventId.value
                payload
            }.map { payload ->
                ScheduleTaskView(
                    campaignId = payload["campaignId"]?.asLong(),
                    taskName = payload["eventId"] as String,
                    templateId = payload["templateId"].asLong(),
                    userIds = (payload["userIds"] as? List<*>)?.mapNotNull { it.asLong() } ?: emptyList(),
                    expiredTime = (payload["expiredTime"] as String).parseISOExpiredTime()
                )
            }.toList()
    }
}
