package com.manage.crm.infrastructure.scheduler.provider

import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import com.manage.crm.infrastructure.scheduler.ScheduleName
import java.time.LocalDateTime

/**
 * Vendor-independent scheduler abstraction interface.
 * Provides basic operations for schedule management across different providers (AWS, Redis+Kafka, etc.)
 */
interface SchedulerProvider {
    /**
     * Creates a new schedule
     * @param name Unique schedule identifier
     * @param scheduleTime When the schedule should execute
     * @param input Schedule payload containing business logic information
     * @return Schedule creation result
     */
    suspend fun createSchedule(name: String, scheduleTime: LocalDateTime, input: ScheduleInfo): ScheduleCreationResult

    /**
     * Lists all schedules
     * @return List of schedule names
     */
    suspend fun browseSchedules(): List<ScheduleName>

    /**
     * Deletes a schedule by name
     * @param scheduleName Schedule identifier to delete
     */
    suspend fun deleteSchedule(scheduleName: ScheduleName)
}

/**
 * Result of schedule creation operation
 */
sealed class ScheduleCreationResult {
    data class Success(val scheduleId: String) : ScheduleCreationResult()
    data class Failure(val reason: String, val cause: Throwable? = null) : ScheduleCreationResult()
}
