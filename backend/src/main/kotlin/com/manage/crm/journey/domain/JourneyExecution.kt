package com.manage.crm.journey.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("journey_executions")
class JourneyExecution(
    @Id
    var id: Long? = null,
    @Column("journey_id")
    var journeyId: Long,
    @Column("event_id")
    var eventId: Long,
    @Column("user_id")
    var userId: Long,
    @Column("status")
    var status: String,
    @Column("current_step_order")
    var currentStepOrder: Int,
    @Column("last_error")
    var lastError: String? = null,
    @Column("trigger_key")
    var triggerKey: String,
    @Column("started_at")
    var startedAt: LocalDateTime,
    @Column("completed_at")
    var completedAt: LocalDateTime? = null,
    @CreatedDate
    @Column("created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column("updated_at")
    var updatedAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            journeyId: Long,
            eventId: Long,
            userId: Long,
            status: String,
            currentStepOrder: Int,
            triggerKey: String,
            startedAt: LocalDateTime
        ): JourneyExecution {
            return JourneyExecution(
                journeyId = journeyId,
                eventId = eventId,
                userId = userId,
                status = status,
                currentStepOrder = currentStepOrder,
                triggerKey = triggerKey,
                startedAt = startedAt
            )
        }
    }
}
