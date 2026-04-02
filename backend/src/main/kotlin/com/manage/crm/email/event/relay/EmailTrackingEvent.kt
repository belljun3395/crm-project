package com.manage.crm.email.event.relay

import com.manage.crm.email.domain.vo.EmailProviderType
import java.time.LocalDateTime

data class EmailTrackingEvent(
    val eventType: EmailTrackingEventType,
    val messageId: String,
    val destination: String,
    val occurredAt: LocalDateTime,
    val provider: EmailProviderType,
    val metadata: Map<String, String> = emptyMap(),
)

enum class EmailTrackingEventType {
    SEND,
    DELIVERY,
    OPEN,
    CLICK,
    BOUNCE,
    COMPLAINT,
    DELIVERY_DELAY,
    ;

    companion object {
        fun from(value: String?): EmailTrackingEventType? {
            val normalized =
                value
                    ?.replace("-", "")
                    ?.replace("_", "")
                    ?.replace(" ", "")
                    ?.uppercase()
            return when (normalized) {
                "SEND", "SENT" -> SEND
                "DELIVERY", "DELIVERED" -> DELIVERY
                "OPEN", "OPENED" -> OPEN
                "CLICK", "CLICKED" -> CLICK
                "BOUNCE", "BOUNCED", "HARDBOUNCE", "SOFTBOUNCE" -> BOUNCE
                "COMPLAINT", "COMPLAINED", "SPAMCOMPLAINT", "SPAM" -> COMPLAINT
                "DELIVERYDELAY", "DELAYED" -> DELIVERY_DELAY
                else -> null
            }
        }
    }
}
