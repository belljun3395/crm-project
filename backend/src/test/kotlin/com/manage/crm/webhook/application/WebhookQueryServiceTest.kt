package com.manage.crm.webhook.application

import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEventType
import com.manage.crm.webhook.domain.WebhookEvents
import com.manage.crm.webhook.domain.repository.WebhookRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WebhookQueryServiceTest : BehaviorSpec({
    lateinit var webhookRepository: WebhookRepository
    lateinit var webhookQueryService: WebhookQueryService

    beforeTest {
        webhookRepository = mockk()
        webhookQueryService = WebhookQueryService(webhookRepository)
    }

    afterTest { (_, _) ->
        clearMocks(webhookRepository)
    }

    given("get webhook by id") {
        `when`("webhook exists") {
            then("return mapped response") {
                val webhookId = 1L
                val webhook = Webhook.new(
                    id = webhookId,
                    name = "user-created",
                    url = "https://example.com/webhooks",
                    events = WebhookEvents(listOf(WebhookEventType.USER_CREATED, WebhookEventType.EMAIL_SENT)),
                    active = true,
                    createdAt = LocalDateTime.of(2024, 1, 2, 12, 0, 0)
                )

                coEvery { webhookRepository.findById(webhookId) } returns webhook

                val response = webhookQueryService.get(webhookId)

                coVerify(exactly = 1) { webhookRepository.findById(webhookId) }
                response.id shouldBe webhookId
                response.name shouldBe webhook.name
                response.url shouldBe webhook.url
                response.events shouldContainExactly listOf(
                    WebhookEventType.USER_CREATED.value,
                    WebhookEventType.EMAIL_SENT.value
                )
                response.active shouldBe true
                response.createdAt shouldBe webhook.createdAt!!.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }
        }

        `when`("webhook does not exist") {
            then("throw NotFoundByIdException") {
                val webhookId = 99L
                coEvery { webhookRepository.findById(webhookId) } returns null

                shouldThrow<NotFoundByIdException> {
                    runBlocking { webhookQueryService.get(webhookId) }
                }
            }
        }
    }

    given("list webhooks") {
        `when`("repository returns multiple webhooks") {
            then("map results to responses") {
                val webhook1 = Webhook.new(
                    id = 1L,
                    name = "first",
                    url = "https://example.com/1",
                    events = WebhookEvents(listOf(WebhookEventType.USER_CREATED)),
                    active = true,
                    createdAt = LocalDateTime.of(2024, 1, 1, 9, 0, 0)
                )
                val webhook2 = Webhook.new(
                    id = 2L,
                    name = "second",
                    url = "https://example.com/2",
                    events = WebhookEvents(listOf(WebhookEventType.EMAIL_SENT)),
                    active = false,
                    createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
                )

                every { webhookRepository.findAll() } returns flowOf(webhook1, webhook2)

                val responses = webhookQueryService.list()

                responses.size shouldBe 2
                responses[0].id shouldBe webhook1.id!!
                responses[0].events shouldContainExactly listOf(WebhookEventType.USER_CREATED.value)
                responses[1].id shouldBe webhook2.id!!
                responses[1].events shouldContainExactly listOf(WebhookEventType.EMAIL_SENT.value)
                responses[1].active shouldBe false
            }
        }
    }
})
