package com.manage.crm.webhook.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("webhook_delivery_dead_letters")
class WebhookDeadLetter(
    @Id
    var id: Long? = null,
    @Column("webhook_id")
    var webhookId: Long,
    @Column("event_id")
    var eventId: String,
    @Column("event_type")
    var eventType: String,
    @Column("payload_json")
    var payloadJson: String,
    @Column("delivery_status")
    var deliveryStatus: String,
    @Column("attempt_count")
    var attemptCount: Int,
    @Column("response_status")
    var responseStatus: Int? = null,
    @Column("error_message")
    var errorMessage: String? = null,
    @CreatedDate
    @Column("created_at")
    var createdAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            webhookId: Long,
            eventId: String,
            eventType: String,
            payloadJson: String,
            deliveryStatus: String,
            attemptCount: Int,
            responseStatus: Int?,
            errorMessage: String?
        ): WebhookDeadLetter {
            return WebhookDeadLetter(
                webhookId = webhookId,
                eventId = eventId,
                eventType = eventType,
                payloadJson = payloadJson,
                deliveryStatus = deliveryStatus,
                attemptCount = attemptCount,
                responseStatus = responseStatus,
                errorMessage = errorMessage
            )
        }
    }
}
