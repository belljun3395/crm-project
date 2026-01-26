package com.manage.crm.infrastructure.scheduler.event

import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import java.time.LocalDateTime

/**
 * Event representing a scheduled task to be executed.
 * Published to Kafka when a schedule becomes due.
 */
data class ScheduledTaskEvent(
    val scheduleName: String,
    val scheduleTime: LocalDateTime,
    val payload: ScheduleInfo,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
