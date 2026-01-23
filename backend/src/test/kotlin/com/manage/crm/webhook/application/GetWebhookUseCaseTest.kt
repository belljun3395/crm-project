package com.manage.crm.webhook.application

import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.webhook.application.dto.GetWebhookUseCaseIn
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
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GetWebhookUseCaseTest : BehaviorSpec({
    lateinit var webhookRepository: WebhookRepository
    lateinit var getWebhookUseCase: GetWebhookUseCase

    beforeTest {
        webhookRepository = mockk()
        getWebhookUseCase = GetWebhookUseCase(webhookRepository)
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

                val response = getWebhookUseCase.execute(GetWebhookUseCaseIn(webhookId))

                coVerify(exactly = 1) { webhookRepository.findById(webhookId) }
                response.webhook.id shouldBe webhookId
                response.webhook.name shouldBe webhook.name
                response.webhook.url shouldBe webhook.url
                response.webhook.events shouldContainExactly listOf(
                    WebhookEventType.USER_CREATED.value,
                    WebhookEventType.EMAIL_SENT.value
                )
                response.webhook.active shouldBe true
                response.webhook.createdAt shouldBe webhook.createdAt!!.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }
        }

        `when`("webhook does not exist") {
            then("throw NotFoundByIdException") {
                val webhookId = 99L
                coEvery { webhookRepository.findById(webhookId) } returns null

                shouldThrow<NotFoundByIdException> {
                    runBlocking { getWebhookUseCase.execute(GetWebhookUseCaseIn(webhookId)) }
                }
            }
        }
    }
})
