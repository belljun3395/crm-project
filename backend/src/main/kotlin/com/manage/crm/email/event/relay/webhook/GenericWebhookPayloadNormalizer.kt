package com.manage.crm.email.event.relay.webhook

import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.email.event.relay.EmailTrackingEvent
import com.manage.crm.email.event.relay.EmailTrackingEventType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * Generic webhook payload normalizer that extracts common fields from any ESP webhook payload.
 *
 * Expects the following fields in the payload (case-insensitive key lookup):
 * - eventType / event_type / event: the tracking event type (OPEN, CLICK, DELIVERY, etc.)
 * - messageId / message_id: the email message ID
 * - destination / email / recipient / to: the recipient email address
 * - timestamp / occurredAt / occurred_at: the event timestamp (ISO 8601)
 */
@Component
class GenericWebhookPayloadNormalizer : WebhookPayloadNormalizer {
    private val log = KotlinLogging.logger {}

    override fun normalize(payload: Map<String, Any?>): EmailTrackingEvent? {
        val eventTypeRaw = findValue(payload, "eventType", "event_type", "event") ?: return null
        val eventType = EmailTrackingEventType.from(eventTypeRaw.toString()) ?: run {
            log.debug { "Unrecognized email tracking event type: $eventTypeRaw" }
            return null
        }

        val messageId = findValue(payload, "messageId", "message_id")?.toString() ?: run {
            log.warn { "Missing messageId in webhook payload" }
            return null
        }

        val destination = findValue(payload, "destination", "email", "recipient", "to")?.toString() ?: run {
            log.warn { "Missing destination in webhook payload" }
            return null
        }

        val timestampRaw = findValue(payload, "timestamp", "occurredAt", "occurred_at")?.toString()
        val occurredAt = timestampRaw?.let { parseTimestamp(it) } ?: LocalDateTime.now()

        return EmailTrackingEvent(
            eventType = eventType,
            messageId = messageId,
            destination = destination,
            occurredAt = occurredAt,
            provider = EmailProviderType.WEBHOOK,
        )
    }

    private fun findValue(
        payload: Map<String, Any?>,
        vararg keys: String,
    ): Any? = keys.firstNotNullOfOrNull { key -> payload[key] ?: payload[key.lowercase()] }

    private fun parseTimestamp(value: String): LocalDateTime? =
        try {
            ZonedDateTime.parse(value).toLocalDateTime()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(value)
            } catch (_: DateTimeParseException) {
                null
            }
        }
}
