package com.manage.crm.email.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.dto.ScheduleTaskView
import com.manage.crm.email.domain.repository.ScheduledEventRepository
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.ScheduleName
import com.manage.crm.infrastructure.scheduler.provider.ScheduleCreationResult
import com.manage.crm.infrastructure.scheduler.provider.SchedulerProvider
import com.manage.crm.support.asLong
import com.manage.crm.support.parseISOExpiredTime
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

/**
 * Implementation of schedule task service using vendor-independent SchedulerProvider.
 * Supports both AWS EventBridge and Redis+Kafka scheduler backends.
 */
@Component
class ScheduleTaskServiceImpl(
    private val scheduledEventRepository: ScheduledEventRepository,
    private val schedulerProvider: SchedulerProvider,
    private val objectMapper: ObjectMapper
) : ScheduleTaskAllService {
    val log = KotlinLogging.logger {}

    override fun newSchedule(input: NotificationEmailSendTimeOutEventInput): String {
        return runBlocking {
            when (val result = schedulerProvider.createSchedule(
                name = input.eventId.value,
                scheduleTime = input.expiredTime,
                input = input
            )) {
                is ScheduleCreationResult.Success -> {
                    log.info { "Successfully created schedule: ${input.eventId.value}" }
                    input.eventId.value
                }
                is ScheduleCreationResult.Failure -> {
                    log.error(result.cause) { "Failed to create schedule: ${result.reason}" }
                    throw RuntimeException("Failed to create schedule: ${result.reason}", result.cause)
                }
            }
        }
    }

    override fun cancel(scheduleName: String) {
        runBlocking {
            schedulerProvider.deleteSchedule(ScheduleName(scheduleName))
        }
        log.info { "Task $scheduleName is cancelled" }
    }

    override fun reSchedule(input: NotificationEmailSendTimeOutEventInput) {
        cancel(input.eventId.value)
        newSchedule(input)
    }

    override suspend fun browseScheduledTasksView(): List<ScheduleTaskView> {
        val scheduleViews = schedulerProvider.browseSchedules()
            .map { EventId(it.value) }
            .filter { it.value.matches(Regex("[a-f0-9]{8}-([a-f0-9]{4}-){3}[a-f0-9]{12}")) }

        return scheduledEventRepository
            .findAllByEventIdIn(scheduleViews)
            .map {
                // TODO: refactor readValue to readTree
                val payload = objectMapper.readValue(it.eventPayload, Map::class.java).toMutableMap()
                payload["eventId"] = it.eventId.value
                payload
            }.map { payload ->
                ScheduleTaskView(
                    taskName = payload["eventId"] as String,
                    templateId = payload["templateId"].asLong(),
                    userIds = (payload["userIds"] as? List<*>)?.mapNotNull { it.asLong() } ?: emptyList(),
                    expiredTime = (payload["expiredTime"] as String).parseISOExpiredTime()
                )
            }.toList()
    }
}
