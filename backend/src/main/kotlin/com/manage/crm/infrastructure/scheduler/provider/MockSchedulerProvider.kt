package com.manage.crm.infrastructure.scheduler.provider

import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import com.manage.crm.infrastructure.scheduler.ScheduleName
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Mock scheduler provider for testing and development
 * Stores schedules in memory without actual scheduling functionality
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "mock")
class MockSchedulerProvider : SchedulerProvider {
    private val log = KotlinLogging.logger {}
    private val schedules = ConcurrentHashMap<String, DueSchedule>()

    override suspend fun createSchedule(
        name: String,
        scheduleTime: LocalDateTime,
        input: ScheduleInfo,
    ): ScheduleCreationResult =
        try {
            val dueSchedule =
                DueSchedule(
                    name = name,
                    scheduleTime = scheduleTime,
                    payload = input,
                )

            schedules[name] = dueSchedule
            log.info { "Mock scheduler: Created schedule '$name' for $scheduleTime" }
            ScheduleCreationResult.Success(name)
        } catch (ex: Exception) {
            log.error(ex) { "Mock scheduler: Failed to create schedule '$name'" }
            ScheduleCreationResult.Failure("Failed to create mock schedule: ${ex.message}", ex)
        }

    override suspend fun browseSchedules(): List<ScheduleName> = schedules.keys.map { ScheduleName(it) }

    override suspend fun deleteSchedule(scheduleName: ScheduleName) {
        schedules.remove(scheduleName.value)
        log.info { "Mock scheduler: Deleted schedule '${scheduleName.value}'" }
    }

    override suspend fun fetchDueSchedules(): List<DueSchedule> {
        val now = LocalDateTime.now()
        return schedules.values.filter { it.scheduleTime.isBefore(now) || it.scheduleTime.isEqual(now) }
    }

    /**
     * Test utility method to clear all schedules
     */
    fun clearAllSchedules() {
        schedules.clear()
        log.debug { "Mock scheduler: Cleared all schedules" }
    }

    /**
     * Test utility method to get schedule count
     */
    fun getScheduleCount(): Int = schedules.size
}
