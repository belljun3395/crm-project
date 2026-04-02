package com.manage.crm.email.event.relay.webhook

import com.manage.crm.email.event.relay.EmailTrackingEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RestController
@RequestMapping("/api/v1/email/tracking")
class WebhookEmailEventController(
    private val emailTrackingKafkaTemplate: KafkaTemplate<String, EmailTrackingEvent>,
    private val webhookPayloadNormalizer: WebhookPayloadNormalizer,
    @Value("\${email.tracking.webhook.signing-secret:#{null}}")
    private val signingSecret: String?,
    @Value("\${email.tracking.kafka.topic:email-tracking-events}")
    private val topic: String,
) {
    private val log = KotlinLogging.logger {}

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    fun receiveWebhook(
        @RequestBody payload: Map<String, Any?>,
        @RequestHeader(name = "X-Webhook-Signature", required = false) signature: String?,
    ) {
        if (!signingSecret.isNullOrBlank()) {
            verifySignature(payload, signature)
        }

        val event = webhookPayloadNormalizer.normalize(payload)
        if (event == null) {
            log.debug { "Webhook payload could not be normalized or is not a tracked event type: $payload" }
            return
        }

        emailTrackingKafkaTemplate
            .send(topic, event.messageId, event)
            .whenComplete { _, ex ->
                if (ex != null) {
                    log.error(ex) { "Failed to publish email tracking event to Kafka: messageId=${event.messageId}" }
                } else {
                    log.info { "Published email tracking event to Kafka: type=${event.eventType}, messageId=${event.messageId}" }
                }
            }
    }

    private fun verifySignature(
        payload: Map<String, Any?>,
        signature: String?,
    ) {
        if (signature.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing webhook signature")
        }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(signingSecret!!.toByteArray(), "HmacSHA256"))
        val expected = mac.doFinal(payload.toString().toByteArray()).joinToString("") { "%02x".format(it) }
        if (!expected.equals(signature, ignoreCase = true)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature")
        }
    }
}
