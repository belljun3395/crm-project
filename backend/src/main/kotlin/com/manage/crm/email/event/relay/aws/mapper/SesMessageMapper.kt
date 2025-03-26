package com.manage.crm.email.event.relay.aws.mapper

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.email.domain.vo.SentEmailStatus
import com.manage.crm.email.event.send.EmailClickEvent
import com.manage.crm.email.event.send.EmailDeliveryDelayEvent
import com.manage.crm.email.event.send.EmailDeliveryEvent
import com.manage.crm.email.event.send.EmailOpenEvent
import com.manage.crm.email.event.send.EmailSendEvent
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

fun ObjectMapper.readMessage(message: String): JsonNode = readTree(message)["Message"]!!

fun ObjectMapper.readMail(message: String): JsonNode = readTree(message)["mail"]!!

fun ObjectMapper.readEventType(message: String): String = readTree(message)["eventType"].asText().toUpperCase()

fun JsonNode.messageId(): String = this["messageId"].asText()

fun JsonNode.timestamp(): LocalDateTime = (this["timestamp"].asText()).let { ZonedDateTime.parse(it).toLocalDateTime() }

fun JsonNode.destination(): String = this["destination"].first().asText()

data class SesMessage(
    val status: SentEmailStatus,
    val messageId: String,
    val destination: String,
    val timestamp: LocalDateTime
)

@Component
class SesMessageMapper(
    private val objectMapper: ObjectMapper
) {
    fun map(message: String): SesMessage {
        objectMapper
            .readMessage(message)
            .let {
                val mail = objectMapper.readMail(it.asText())
                val eventType = SentEmailStatus.valueOf(objectMapper.readEventType(it.asText()))
                return SesMessage(
                    status = eventType,
                    messageId = mail.messageId(),
                    destination = mail.destination(),
                    timestamp = mail.timestamp()
                )
            }
    }

    fun toEvent(message: SesMessage): Optional<EmailSendEvent> =
        when (message.status) {
            SentEmailStatus.OPEN ->
                Optional.of(
                    EmailOpenEvent(
                        messageId = message.messageId,
                        destination = message.destination,
                        timestamp = message.timestamp,
                        provider = EmailProviderType.AWS
                    )
                )
            SentEmailStatus.DELIVERY ->
                Optional.of(
                    EmailDeliveryEvent(
                        messageId = message.messageId,
                        destination = message.destination,
                        timestamp = message.timestamp,
                        provider = EmailProviderType.AWS
                    )
                )
            SentEmailStatus.CLICK ->
                Optional.of(
                    EmailClickEvent(
                        messageId = message.messageId,
                        destination = message.destination,
                        timestamp = message.timestamp,
                        provider = EmailProviderType.AWS
                    )
                )
            SentEmailStatus.DELIVERYDELAY ->
                Optional.of(
                    EmailDeliveryDelayEvent(
                        messageId = message.messageId,
                        destination = message.destination,
                        timestamp = message.timestamp,
                        provider = EmailProviderType.AWS
                    )
                )
            else -> Optional.empty()
        }
}
