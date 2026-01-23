package com.manage.crm.email.event.relay.aws.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.event.relay.aws.model.SesEventMessage
import com.manage.crm.email.event.relay.aws.model.SesEventType
import com.manage.crm.email.event.relay.aws.model.SesSnsNotification
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

data class SesEmailNotification(
    val eventType: SesEventType,
    val messageId: String,
    val destination: String,
    val occurredAt: LocalDateTime
)

@Component
class SesMessageMapper(
    private val objectMapper: ObjectMapper
) {

    fun map(message: String): SesEmailNotification {
        val snsNotification = objectMapper.readValue(message, SesSnsNotification::class.java)
        val eventPayload = objectMapper.readValue(snsNotification.rawMessage, SesEventMessage::class.java)

        val resolvedEventType = eventPayload.resolvedEventType
            ?: throw IllegalArgumentException("Unsupported SES event without eventType/notificationType: ${snsNotification.messageId}")

        val timestamp = eventPayload.resolveTimestamp()
            ?: throw IllegalArgumentException("SES event missing timestamp for type: $resolvedEventType")

        val destination = eventPayload.mail.destination.firstOrNull()
            ?: eventPayload.mail.commonHeaders?.to?.firstOrNull()
            ?: throw IllegalArgumentException("SES event missing destination: ${eventPayload.mail}")

        return SesEmailNotification(
            eventType = resolvedEventType,
            messageId = eventPayload.mail.messageId,
            destination = destination,
            occurredAt = timestamp
        )
    }

    private fun SesEventMessage.resolveTimestamp(): LocalDateTime? {
        val candidateTimestamp = delivery?.timestamp
            ?: open?.timestamp
            ?: click?.timestamp
            ?: deliveryDelay?.timestamp
            ?: bounce?.timestamp
            ?: complaint?.timestamp
            ?: send?.timestamp
            ?: mail.timestamp
        return candidateTimestamp?.let(::parseToLocalDateTime)
    }

    private fun parseToLocalDateTime(value: String): LocalDateTime? {
        return try {
            ZonedDateTime.parse(value).toLocalDateTime()
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(value).toLocalDateTime()
            } catch (_: DateTimeParseException) {
                try {
                    Instant.parse(value).atOffset(ZoneOffset.UTC).toLocalDateTime()
                } catch (_: DateTimeParseException) {
                    null
                }
            }
        }
    }
}
