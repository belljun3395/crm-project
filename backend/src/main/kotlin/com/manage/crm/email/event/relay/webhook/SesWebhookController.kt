package com.manage.crm.email.event.relay.webhook

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.event.relay.aws.SesEmailEventFactory
import com.manage.crm.email.event.relay.aws.mapper.SesMessageMapper
import com.manage.crm.email.support.EmailEventPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URL

@RestController
@RequestMapping("/api/webhooks/ses")
class SesWebhookController(
    private val emailEventPublisher: EmailEventPublisher,
    private val eventMessageMapper: SesMessageMapper,
    private val sesEmailEventFactory: SesEmailEventFactory,
    private val objectMapper: ObjectMapper
) {
    private val log = KotlinLogging.logger {}

    @PostMapping
    fun handleNotification(
        @RequestHeader("x-amz-sns-message-type") messageType: String,
        @RequestBody body: String
    ) {
        log.info { "Received SNS webhook message of type: $messageType" }

        when (messageType) {
            "SubscriptionConfirmation" -> handleSubscriptionConfirmation(body)
            "Notification" -> handleSnsNotification(body)
            else -> log.warn { "Unhandled SNS message type: $messageType" }
        }
    }

    private fun handleSubscriptionConfirmation(body: String) {
        val node = objectMapper.readTree(body)
        val subscribeUrl = node["SubscribeURL"].asText()
        log.info { "SNS Subscription Confirmation URL: $subscribeUrl" }

        runCatching {
            URL(subscribeUrl).readText()
            log.info { "Successfully confirmed SNS subscription" }
        }.onFailure { e ->
            log.error(e) { "Failed to confirm SNS subscription" }
        }
    }

    private fun handleSnsNotification(body: String) {
        try {
            eventMessageMapper.map(body)
                .let { sesEmailEventFactory.toEmailSendEvent(it) }
                .ifPresent { emailEventPublisher.publishEvent(it) }
        } catch (e: Exception) {
            log.error(e) { "Error processing SES webhook notification" }
        }
    }
}
