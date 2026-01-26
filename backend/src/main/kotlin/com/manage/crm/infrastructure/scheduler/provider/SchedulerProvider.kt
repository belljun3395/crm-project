package com.manage.crm.infrastructure.scheduler.provider

import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import com.manage.crm.infrastructure.scheduler.ScheduleName
import java.time.LocalDateTime

/**
 * Vendor-independent interface for scheduling operations.
 * Implementations can use AWS EventBridge, Redis+Kafka, or other scheduling backends.
 */
interface SchedulerProvider {
    /**
     * Creates a new schedule.
     *
     * @param name Unique name for the schedule
     * @param scheduleTime When the schedule should trigger
     * @param input Schedule payload information
     * @return Result indicating success with schedule ID or failure with reason
     */
    suspend fun createSchedule(
        name: String,
        scheduleTime: LocalDateTime,
        input: ScheduleInfo
    ): ScheduleCreationResult

    /**
     * Lists all active schedules.
     *
     * @return List of schedule names
     */
    suspend fun browseSchedules(): List<ScheduleName>

    /**
     * Deletes a schedule by name.
     *
     * @param scheduleName The schedule to delete
     */
    suspend fun deleteSchedule(scheduleName: ScheduleName)
}

/**
 * Result type for schedule creation operations.
 */
sealed class ScheduleCreationResult {
    data class Success(val scheduleId: String) : ScheduleCreationResult()
    data class Failure(val reason: String, val cause: Throwable? = null) : ScheduleCreationResult()
}
