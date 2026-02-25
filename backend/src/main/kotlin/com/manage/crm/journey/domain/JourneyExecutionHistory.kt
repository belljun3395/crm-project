package com.manage.crm.journey.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("journey_execution_histories")
class JourneyExecutionHistory(
    @Id
    var id: Long? = null,
    @Column("journey_execution_id")
    var journeyExecutionId: Long,
    @Column("journey_step_id")
    var journeyStepId: Long,
    @Column("status")
    var status: String,
    @Column("attempt")
    var attempt: Int,
    @Column("message")
    var message: String? = null,
    @Column("idempotency_key")
    var idempotencyKey: String? = null,
    @CreatedDate
    @Column("created_at")
    var createdAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            journeyExecutionId: Long,
            journeyStepId: Long,
            status: String,
            attempt: Int,
            message: String?,
            idempotencyKey: String?
        ): JourneyExecutionHistory {
            return JourneyExecutionHistory(
                journeyExecutionId = journeyExecutionId,
                journeyStepId = journeyStepId,
                status = status,
                attempt = attempt,
                message = message,
                idempotencyKey = idempotencyKey
            )
        }
    }
}
