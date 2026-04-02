package com.manage.crm.email.event.relay.webhook

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.event.relay.EmailTrackingEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Receives ESP webhook callbacks and publishes them to the email-tracking-events Kafka topic.
 *
 * Activated only when `email.tracking.provider=webhook`.
 * Requires `email.tracking.webhook.signing-secret` to be configured; startup fails if missing.
 */
@ConditionalOnProperty(name = ["email.tracking.provider"], havingValue = "webhook")
@RestController
@RequestMapping("/api/v1/email/tracking")
class WebhookEmailEventController(
    private val emailTrackingKafkaTemplate: KafkaTemplate<String, EmailTrackingEvent>,
    private val webhookPayloadNormalizer: WebhookPayloadNormalizer,
    private val objectMapper: ObjectMapper,
    @Value("\${email.tracking.webhook.signing-secret}")
    private val signingSecret: String,
    @Value("\${email.tracking.kafka.topic:email-tracking-events}")
    private val topic: String,
) {
    private val log = KotlinLogging.logger {}

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    suspend fun receiveWebhook(
        @RequestBody rawBody: String,
        @RequestHeader(name = "X-Webhook-Signature", required = false) signature: String?,
    ) {
        verifySignature(rawBody.toByteArray(Charsets.UTF_8), signature)

        @Suppress("UNCHECKED_CAST")
        val payload = objectMapper.readValue(rawBody, Map::class.java) as Map<String, Any?>

        val event =
            webhookPayloadNormalizer.normalize(payload)
                ?: run {
                    log.debug { "Webhook payload could not be normalized or is not a tracked event type" }
                    return
                }

        try {
            Mono
                .fromFuture(emailTrackingKafkaTemplate.send(topic, event.messageId, event))
                .awaitSingle()
            log.info { "Published email tracking event to Kafka: type=${event.eventType}, messageId=${event.messageId}" }
        } catch (ex: Exception) {
            log.error(ex) { "Failed to publish email tracking event to Kafka: messageId=${event.messageId}" }
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed to publish event to message broker")
        }
    }

    private fun verifySignature(
        rawBody: ByteArray,
        signature: String?,
    ) {
        if (signature.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing webhook signature")
        }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(signingSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val expected = mac.doFinal(rawBody).joinToString("") { "%02x".format(it) }
        if (!MessageDigest.isEqual(
                expected.lowercase().toByteArray(Charsets.UTF_8),
                signature.lowercase().toByteArray(Charsets.UTF_8),
            )
        ) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature")
        }
    }
}
