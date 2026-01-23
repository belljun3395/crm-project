package com.manage.crm.webhook.application

import com.manage.crm.webhook.WebhookClient
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEventPayload
import com.manage.crm.webhook.domain.WebhookEventType
import com.manage.crm.webhook.domain.WebhookEvents
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
    lateinit var webhookDispatchService: WebhookDispatchService

    beforeTest {
        webhookRepository = mockk()
        webhookClient = mockk(relaxed = true)
        webhookDispatchService = WebhookDispatchService(webhookRepository, webhookClient)
    }

    given("dispatching webhook events") {
        `when`("no active webhook exists for the event") {
            then("skip sending payload") {
                coEvery { webhookRepository.findActiveByEvent(WebhookEventType.USER_CREATED.value) } returns emptyList()

                webhookDispatchService.dispatch(WebhookEventType.USER_CREATED, mapOf("userId" to 1L))

                coVerify(exactly = 0) { webhookClient.send(any(), any()) }
            }
        }

        `when`("active webhooks exist for the event") {
            then("send payload to each active webhook") {
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
                coEvery { webhookClient.send(any(), any()) } returns Unit

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
                coVerify(exactly = 1) { webhookRepository.findActiveByEvent(WebhookEventType.USER_CREATED.value) }
            }
        }
    }
})
