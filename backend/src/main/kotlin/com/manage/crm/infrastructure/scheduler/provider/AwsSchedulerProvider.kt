package com.manage.crm.infrastructure.scheduler.provider

import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import com.manage.crm.infrastructure.scheduler.ScheduleName
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * AWS EventBridge Scheduler based implementation of SchedulerProvider.
 * Wraps AwsSchedulerService to provide vendor-independent interface.
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "aws")
@ConditionalOnBean(AwsSchedulerService::class)
class AwsSchedulerProvider(
    private val awsSchedulerService: AwsSchedulerService
) : SchedulerProvider {

    override suspend fun createSchedule(
        name: String,
        scheduleTime: LocalDateTime,
        input: ScheduleInfo
    ): ScheduleCreationResult {
        return try {
            val response = awsSchedulerService.createSchedule(name, scheduleTime, input)
            ScheduleCreationResult.Success(response.scheduleArn())
        } catch (ex: Exception) {
            ScheduleCreationResult.Failure(ex.message ?: "Unknown error", ex)
        }
    }

    override suspend fun browseSchedules(): List<ScheduleName> {
        return awsSchedulerService.browseSchedule()
    }

    override suspend fun deleteSchedule(scheduleName: ScheduleName) {
        awsSchedulerService.deleteSchedule(scheduleName)
    }
}
