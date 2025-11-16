package com.manage.crm.email.event.relay.aws.mapper

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.manage.crm.email.event.relay.aws.SesEmailEventFactory
import com.manage.crm.email.event.relay.aws.model.SesEventType
import com.manage.crm.email.event.send.EmailDeliveryDelayEvent
import com.manage.crm.email.event.send.EmailDeliveryEvent
import com.manage.crm.email.event.send.EmailOpenEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class SesMessageMapperTest {

    private val objectMapper = jacksonObjectMapper()
    private val mapper = SesMessageMapper(objectMapper)
    private val factory = SesEmailEventFactory()

    @Test
    fun `map open event payload`() {
        val messageId = "0000000000000001"
        val recipient = "recipient@example.com"
        val eventTimestamp = "2023-10-24T12:34:56.789Z"

        val snsMessage = snsMessage(
            eventPayload = mapOf(
                "eventType" to "Open",
                "mail" to mapOf(
                    "timestamp" to eventTimestamp,
                    "messageId" to messageId,
                    "source" to "sender@example.com",
                    "destination" to listOf(recipient)
                ),
                "open" to mapOf(
                    "timestamp" to eventTimestamp,
                    "ipAddress" to "192.0.2.1",
                    "userAgent" to "Mozilla/5.0"
                )
            )
        )

        val notification = mapper.map(snsMessage)

        assertEquals(SesEventType.OPEN, notification.eventType)
        assertEquals(messageId, notification.messageId)
        assertEquals(recipient, notification.destination)
        assertEquals(eventTimestamp.toLocalDateTime(), notification.occurredAt)

        val domainEvent = factory.toEmailSendEvent(notification)
        assertTrue(domainEvent.isPresent)
        assertTrue(domainEvent.get() is EmailOpenEvent)
    }

    @Test
    fun `map delivery event payload`() {
        val messageId = "0000000000000002"
        val recipient = "delivered@example.com"
        val mailTimestamp = "2023-10-25T08:15:30.000Z"
        val deliveryTimestamp = "2023-10-25T08:15:32.123Z"

        val snsMessage = snsMessage(
            eventPayload = mapOf(
                "eventType" to "Delivery",
                "mail" to mapOf(
                    "timestamp" to mailTimestamp,
                    "messageId" to messageId,
                    "source" to "sender@example.com",
                    "destination" to listOf(recipient)
                ),
                "delivery" to mapOf(
                    "timestamp" to deliveryTimestamp,
                    "processingTimeMillis" to 200,
                    "recipients" to listOf(recipient),
                    "smtpResponse" to "250 OK"
                )
            )
        )

        val notification = mapper.map(snsMessage)

        assertEquals(SesEventType.DELIVERY, notification.eventType)
        assertEquals(deliveryTimestamp.toLocalDateTime(), notification.occurredAt)

        val domainEvent = factory.toEmailSendEvent(notification)
        assertTrue(domainEvent.isPresent)
        assertTrue(domainEvent.get() is EmailDeliveryEvent)
    }

    @Test
    fun `map delivery delay event payload`() {
        val messageId = "0000000000000003"
        val recipient = "delay@example.com"
        val delayTimestamp = "2023-10-26T11:22:33.444Z"

        val snsMessage = snsMessage(
            eventPayload = mapOf(
                "eventType" to "DeliveryDelay",
                "mail" to mapOf(
                    "timestamp" to delayTimestamp,
                    "messageId" to messageId,
                    "source" to "sender@example.com",
                    "destination" to listOf(recipient)
                ),
                "deliveryDelay" to mapOf(
                    "timestamp" to delayTimestamp,
                    "delayedRecipients" to listOf(
                        mapOf(
                            "emailAddress" to recipient,
                            "delayType" to "General"
                        )
                    ),
                    "diagnosticCode" to "Temporary failure"
                )
            )
        )

        val notification = mapper.map(snsMessage)

        assertEquals(SesEventType.DELIVERY_DELAY, notification.eventType)
        assertEquals(delayTimestamp.toLocalDateTime(), notification.occurredAt)

        val domainEvent = factory.toEmailSendEvent(notification)
        assertTrue(domainEvent.isPresent)
        assertTrue(domainEvent.get() is EmailDeliveryDelayEvent)
    }

    @Test
    fun `factory should ignore bounce events`() {
        val messageId = "0000000000000004"
        val recipient = "bounce@example.com"
        val eventTimestamp = "2023-10-27T10:00:00.000Z"

        val snsMessage = snsMessage(
            eventPayload = mapOf(
                "eventType" to "Bounce",
                "mail" to mapOf(
                    "timestamp" to eventTimestamp,
                    "messageId" to messageId,
                    "source" to "sender@example.com",
                    "destination" to listOf(recipient)
                ),
                "bounce" to mapOf(
                    "timestamp" to eventTimestamp,
                    "bounceType" to "Permanent",
                    "bounceSubType" to "General",
                    "bouncedRecipients" to listOf(
                        mapOf(
                            "emailAddress" to recipient,
                            "action" to "failed",
                            "status" to "5.0.0"
                        )
                    )
                )
            )
        )

        val notification = mapper.map(snsMessage)

        assertEquals(SesEventType.BOUNCE, notification.eventType)
        val domainEvent = factory.toEmailSendEvent(notification)
        assertTrue(domainEvent.isEmpty)
    }

    private fun snsMessage(
        eventPayload: Map<String, Any?>,
        snsTimestamp: String = Instant.now().toString()
    ): String {
        val snsEnvelope = mapOf(
            "Type" to "Notification",
            "MessageId" to UUID.randomUUID().toString(),
            "TopicArn" to "arn:aws:sns:us-east-1:123456789012:ses-events",
            "Subject" to "Amazon SES Email Event",
            "Message" to objectMapper.writeValueAsString(eventPayload),
            "Timestamp" to snsTimestamp,
            "SignatureVersion" to "1",
            "Signature" to "EXAMPLE",
            "SigningCertURL" to "https://example.com/cert.pem",
            "UnsubscribeURL" to "https://example.com/unsubscribe"
        )
        return objectMapper.writeValueAsString(snsEnvelope)
    }

    private fun String.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(Instant.parse(this), ZoneOffset.UTC)
}
