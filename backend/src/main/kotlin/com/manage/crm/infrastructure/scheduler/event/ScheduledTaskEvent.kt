package com.manage.crm.infrastructure.scheduler.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import java.time.LocalDateTime

/**
 * Event payload for scheduled task execution via Kafka
 */
data class ScheduledTaskEvent(
    val scheduleName: String,
    val scheduleTime: LocalDateTime,
    val payload: ScheduleInfo,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Wrapper for polymorphic ScheduleInfo serialization
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
@JsonSubTypes(
    // Add concrete ScheduleInfo types here as they are implemented
    // Example: JsonSubTypes.Type(value = EmailScheduleInfo::class, name = "email")
)
abstract class ScheduleInfoMixin
