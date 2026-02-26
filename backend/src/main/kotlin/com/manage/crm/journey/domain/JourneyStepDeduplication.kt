package com.manage.crm.journey.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("journey_step_deduplications")
class JourneyStepDeduplication(
    @Id
    var id: Long? = null,
    @Column("idempotency_key")
    var idempotencyKey: String,
    @CreatedDate
    @Column("created_at")
    var createdAt: LocalDateTime? = null
) {
    companion object {
        fun new(idempotencyKey: String): JourneyStepDeduplication {
            return JourneyStepDeduplication(idempotencyKey = idempotencyKey)
        }
    }
}
