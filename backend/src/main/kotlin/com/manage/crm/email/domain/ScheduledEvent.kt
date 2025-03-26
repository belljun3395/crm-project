package com.manage.crm.email.domain

import com.manage.crm.email.domain.vo.EventId
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

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
    val scheduledAt: String? = null
) {
    fun complete(): ScheduledEvent {
        completed = true
        return this
    }

    fun notConsumed(): ScheduledEvent {
        isNotConsumed = true
        return this
    }

    fun cancel(): ScheduledEvent {
        completed = true
        isNotConsumed = true
        canceled = true
        return this
    }
}
