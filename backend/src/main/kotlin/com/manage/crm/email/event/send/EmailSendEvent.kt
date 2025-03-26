package com.manage.crm.email.event.send

import com.manage.crm.email.domain.vo.EmailProviderType
import java.time.LocalDateTime

abstract class EmailSendEvent(
    val messageId: String,
    val destination: String,
    val timestamp: LocalDateTime,
    val provider: EmailProviderType
) {
    override fun toString(): String {
        return """
            {
                "messageId": "$messageId",
                "destination": "$destination",
                "timestamp": "$timestamp",
                "provider": "$provider"
            }
        """.trimIndent()
    }
}

// ----------------- Email Send Status Event -----------------
class EmailSentEvent(
    val userId: Long,
    val emailBody: String,
    messageId: String,
    destination: String,
    timestamp: LocalDateTime = LocalDateTime.now(),
    provider: EmailProviderType
) : EmailSendEvent(
    messageId = messageId,
    destination = destination,
    timestamp = timestamp,
    provider = provider
)

class EmailDeliveryEvent(
    messageId: String,
    destination: String,
    timestamp: LocalDateTime,
    provider: EmailProviderType
) : EmailSendEvent(
    messageId = messageId,
    destination = destination,
    timestamp = timestamp,
    provider = provider
)

class EmailOpenEvent(
    messageId: String,
    destination: String,
    timestamp: LocalDateTime,
    provider: EmailProviderType
) : EmailSendEvent(
    messageId = messageId,
    destination = destination,
    timestamp = timestamp,
    provider = provider
)

class EmailClickEvent(
    messageId: String,
    destination: String,
    timestamp: LocalDateTime,
    provider: EmailProviderType
) : EmailSendEvent(
    messageId = messageId,
    destination = destination,
    timestamp = timestamp,
    provider = provider
)

class EmailDeliveryDelayEvent(
    messageId: String,
    destination: String,
    timestamp: LocalDateTime,
    provider: EmailProviderType
) : EmailSendEvent(
    messageId = messageId,
    destination = destination,
    timestamp = timestamp,
    provider = provider
)
