package com.manage.crm.journey.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("journey_steps")
class JourneyStep(
    @Id
    var id: Long? = null,
    @Column("journey_id")
    var journeyId: Long,
    @Column("step_order")
    var stepOrder: Int,
    @Column("step_type")
    var stepType: String,
    @Column("channel")
    var channel: String? = null,
    @Column("destination")
    var destination: String? = null,
    @Column("subject")
    var subject: String? = null,
    @Column("body")
    var body: String? = null,
    @Column("variables_json")
    var variablesJson: String? = null,
    @Column("delay_millis")
    var delayMillis: Long? = null,
    @Column("condition_expression")
    var conditionExpression: String? = null,
    @Column("retry_count")
    var retryCount: Int = 0,
    @CreatedDate
    @Column("created_at")
    var createdAt: LocalDateTime? = null,
) {
    companion object {
        fun new(
            journeyId: Long,
            stepOrder: Int,
            stepType: String,
            channel: String?,
            destination: String?,
            subject: String?,
            body: String?,
            variablesJson: String?,
            delayMillis: Long?,
            conditionExpression: String?,
            retryCount: Int,
        ): JourneyStep =
            JourneyStep(
                journeyId = journeyId,
                stepOrder = stepOrder,
                stepType = stepType,
                channel = channel,
                destination = destination,
                subject = subject,
                body = body,
                variablesJson = variablesJson,
                delayMillis = delayMillis,
                conditionExpression = conditionExpression,
                retryCount = retryCount,
            )
    }
}
