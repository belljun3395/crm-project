package com.manage.crm.webhook.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

/**
 * Persistence model for a single webhook delivery attempt and its outcome.
 */
@Table("webhook_delivery_logs")
class WebhookDeliveryLog(
    @Id
    var id: Long? = null,
    @Column("webhook_id")
    var webhookId: Long,
    @Column("event_id")
    var eventId: String,
    @Column("event_type")
    var eventType: String,
    @Column("delivery_status")
    var deliveryStatus: String,
    @Column("attempt_count")
    var attemptCount: Int,
    @Column("response_status")
    var responseStatus: Int? = null,
    @Column("error_message")
    var errorMessage: String? = null,
    @CreatedDate
    @Column("delivered_at")
    var deliveredAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            webhookId: Long,
            eventId: String,
            eventType: String,
            deliveryStatus: String,
            attemptCount: Int,
            responseStatus: Int?,
            errorMessage: String?
        ): WebhookDeliveryLog {
            return WebhookDeliveryLog(
                webhookId = webhookId,
                eventId = eventId,
                eventType = eventType,
                deliveryStatus = deliveryStatus,
                attemptCount = attemptCount,
                responseStatus = responseStatus,
                errorMessage = errorMessage
            )
        }
    }
}
