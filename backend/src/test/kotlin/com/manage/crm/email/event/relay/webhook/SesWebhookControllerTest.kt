package com.manage.crm.email.event.relay.webhook

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.email.event.relay.aws.SesEmailEventFactory
import com.manage.crm.email.event.relay.aws.mapper.SesEmailNotification
import com.manage.crm.email.event.relay.aws.mapper.SesMessageMapper
import com.manage.crm.email.event.relay.aws.model.SesEventType
import com.manage.crm.email.event.send.EmailDeliveryEvent
import com.manage.crm.email.support.EmailEventPublisher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDateTime
import java.util.Optional

class SesWebhookControllerTest {
    private val emailEventPublisher = mockk<EmailEventPublisher>(relaxed = true)
    private val eventMessageMapper = mockk<SesMessageMapper>()
    private val sesEmailEventFactory = mockk<SesEmailEventFactory>()
    private val objectMapper = ObjectMapper().registerKotlinModule()

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        val controller = SesWebhookController(
            emailEventPublisher,
            eventMessageMapper,
            sesEmailEventFactory,
            objectMapper
        )
        webTestClient = WebTestClient.bindToController(controller).build()
    }

    @Test
    fun `should handle Notification message and publish event`() {
        // Given
        val body = """{"Type":"Notification","Message":"..."}"""
        val notification = SesEmailNotification(
            eventType = SesEventType.DELIVERY,
            messageId = "msg-123",
            destination = "test@example.com",
            occurredAt = LocalDateTime.now()
        )
        val deliveryEvent = EmailDeliveryEvent(
            messageId = notification.messageId,
            destination = notification.destination,
            timestamp = notification.occurredAt,
            provider = EmailProviderType.AWS
        )

        every { eventMessageMapper.map(body) } returns notification
        every { sesEmailEventFactory.toEmailSendEvent(notification) } returns Optional.of(deliveryEvent)

        // When & Then
        webTestClient.post()
            .uri("/api/webhooks/ses")
            .header("x-amz-sns-message-type", "Notification")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isOk

        verify(exactly = 1) { emailEventPublisher.publishEvent(any<EmailDeliveryEvent>()) }
    }

    @Test
    fun `should log warning for unhandled SNS message types`() {
        // When & Then
        webTestClient.post()
            .uri("/api/webhooks/ses")
            .header("x-amz-sns-message-type", "UnsubscribeConfirmation")
            .bodyValue("{}")
            .exchange()
            .expectStatus().isOk

        verify(exactly = 0) { emailEventPublisher.publishEvent(any<EmailDeliveryEvent>()) }
    }
}
