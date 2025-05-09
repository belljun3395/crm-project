package com.manage.crm.email.domain

import com.manage.crm.email.domain.vo.EventId
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table(name = "scheduled_events")
class ScheduledEvent(
    @Id
    var id: Long? = null,
    @Column("event_id")
    val eventId: EventId? = null,
    @Column("event_class")
    val eventClass: String? = null,
    @Column("event_payload")
    val eventPayload: String? = null,
    @Column("completed")
    var completed: Boolean = false,
    @Column("is_not_consumed")
    var isNotConsumed: Boolean = false,
    @Column("canceled")
    var canceled: Boolean = false,
    @Column("scheduled_at")
    val scheduledAt: String? = null,
    @CreatedDate
    var createdAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            eventId: EventId,
            eventClass: String,
            eventPayload: String,
            completed: Boolean,
            scheduledAt: String
        ): ScheduledEvent {
            return ScheduledEvent(
                eventId = eventId,
                eventClass = eventClass,
                eventPayload = eventPayload,
                completed = completed,
                scheduledAt = scheduledAt
            )
        }

        fun new(
            id: Long,
            eventId: EventId,
            eventClass: String,
            eventPayload: String,
            completed: Boolean,
            isNotConsumed: Boolean,
            canceled: Boolean,
            scheduledAt: String,
            createdAt: LocalDateTime
        ): ScheduledEvent {
            return ScheduledEvent(
                id = id,
                eventId = eventId,
                eventClass = eventClass,
                eventPayload = eventPayload,
                completed = completed,
                isNotConsumed = isNotConsumed,
                canceled = canceled,
                scheduledAt = scheduledAt,
                createdAt = createdAt
            )
        }
    }

    /**
     * Mark the event as completed.
     */
    fun complete(): ScheduledEvent {
        completed = true
        return this
    }

    /**
     * Mark the event as not consumed.
     */
    fun notConsumed(): ScheduledEvent {
        isNotConsumed = true
        return this
    }

    /**
     * Mark the event as canceled.
     */
    fun cancel(): ScheduledEvent {
        completed = true
        isNotConsumed = true
        canceled = true
        return this
    }
}
