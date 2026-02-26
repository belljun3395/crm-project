package com.manage.crm.webhook.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.manage.crm.webhook.WebhookClient
import com.manage.crm.webhook.WebhookDeliveryResult
import com.manage.crm.webhook.WebhookDeliveryStatus
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEventPayload
import com.manage.crm.webhook.domain.WebhookEventType
import com.manage.crm.webhook.domain.WebhookEvents
import com.manage.crm.webhook.domain.repository.WebhookDeadLetterRepository
import com.manage.crm.webhook.domain.repository.WebhookDeliveryLogRepository
import com.manage.crm.webhook.domain.repository.WebhookRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDateTime

class WebhookDispatchServiceTest : BehaviorSpec({
    lateinit var webhookRepository: WebhookRepository
    lateinit var webhookClient: WebhookClient
    lateinit var webhookDeliveryLogRepository: WebhookDeliveryLogRepository
    lateinit var webhookDeadLetterRepository: WebhookDeadLetterRepository
    lateinit var webhookDispatchService: WebhookDispatchService

    beforeTest {
        webhookRepository = mockk()
        webhookClient = mockk()
        webhookDeliveryLogRepository = mockk(relaxed = true)
        webhookDeadLetterRepository = mockk(relaxed = true)
        webhookDispatchService = WebhookDispatchService(
            webhookRepository = webhookRepository,
            webhookClient = webhookClient,
            webhookDeliveryLogRepository = webhookDeliveryLogRepository,
            webhookDeadLetterRepository = webhookDeadLetterRepository,
            objectMapper = jacksonObjectMapper()
        )
    }

    given("dispatching webhook events") {
        `when`("no active webhook exists for the event") {
            then("skip sending payload") {
                coEvery { webhookRepository.findActiveByEvent(WebhookEventType.USER_CREATED.value) } returns emptyList()

                webhookDispatchService.dispatch(WebhookEventType.USER_CREATED, mapOf("userId" to 1L))

                coVerify(exactly = 0) { webhookClient.send(any(), any()) }
                coVerify(exactly = 0) { webhookDeliveryLogRepository.save(any()) }
                coVerify(exactly = 0) { webhookDeadLetterRepository.save(any()) }
            }
        }

        `when`("active webhooks exist for the event") {
            then("send payload to each active webhook and persist delivery logs") {
                val webhook1 = Webhook.new(
                    id = 1L,
                    name = "first",
                    url = "https://example.com/1",
                    events = WebhookEvents(listOf(WebhookEventType.USER_CREATED)),
                    active = true,
                    createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
                )
                val webhook2 = Webhook.new(
                    id = 2L,
                    name = "second",
                    url = "https://example.com/2",
                    events = WebhookEvents(listOf(WebhookEventType.USER_CREATED)),
                    active = true,
                    createdAt = LocalDateTime.of(2024, 1, 1, 11, 0, 0)
                )

                coEvery { webhookRepository.findActiveByEvent(WebhookEventType.USER_CREATED.value) } returns listOf(
                    webhook1,
                    webhook2
                )
                coEvery { webhookClient.send(any(), any()) } returns WebhookDeliveryResult(
                    status = WebhookDeliveryStatus.SUCCESS,
                    attemptCount = 1,
                    responseStatus = 200
                )

                val payload = mapOf("userId" to 1L, "source" to "test")
                webhookDispatchService.dispatch(WebhookEventType.USER_CREATED, payload)

                val payloadSlot = slot<WebhookEventPayload>()
                coVerify(timeout = 1000, exactly = 1) {
                    webhookClient.send(webhook1, capture(payloadSlot))
                }
                payloadSlot.captured.eventType shouldBe WebhookEventType.USER_CREATED.value
                payloadSlot.captured.data shouldBe payload
                payloadSlot.captured.eventId.isNotBlank().shouldBeTrue()
                payloadSlot.captured.occurredAt.isNotBlank().shouldBeTrue()

                coVerify(timeout = 1000, exactly = 1) { webhookClient.send(webhook2, any()) }
                coVerify(timeout = 1000, exactly = 2) { webhookDeliveryLogRepository.save(any()) }
                coVerify(exactly = 0) { webhookDeadLetterRepository.save(any()) }
                coVerify(exactly = 1) { webhookRepository.findActiveByEvent(WebhookEventType.USER_CREATED.value) }
            }
        }

        `when`("delivery fails after retries") {
            then("save dead letter for follow-up processing") {
                val webhook = Webhook.new(
                    id = 10L,
                    name = "failed-target",
                    url = "https://example.com/fail",
                    events = WebhookEvents(listOf(WebhookEventType.USER_CREATED)),
                    active = true,
                    createdAt = LocalDateTime.of(2024, 1, 2, 10, 0, 0)
                )

                coEvery { webhookRepository.findActiveByEvent(WebhookEventType.USER_CREATED.value) } returns listOf(webhook)
                coEvery { webhookClient.send(any(), any()) } returns WebhookDeliveryResult(
                    status = WebhookDeliveryStatus.FAILED,
                    attemptCount = 3,
                    responseStatus = 500,
                    errorMessage = "timeout"
                )

                webhookDispatchService.dispatch(
                    WebhookEventType.USER_CREATED,
                    mapOf("userId" to 100L, "source" to "failed-test")
                )

                coVerify(timeout = 1000, exactly = 1) { webhookClient.send(webhook, any()) }
                coVerify(timeout = 1000, exactly = 1) { webhookDeliveryLogRepository.save(any()) }
                coVerify(timeout = 1000, exactly = 1) { webhookDeadLetterRepository.save(any()) }
            }
        }
    }
})
