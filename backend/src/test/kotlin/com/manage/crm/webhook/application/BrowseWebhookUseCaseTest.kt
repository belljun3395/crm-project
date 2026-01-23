package com.manage.crm.webhook.application

import com.manage.crm.webhook.application.dto.BrowseWebhookUseCaseIn
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEventType
import com.manage.crm.webhook.domain.WebhookEvents
import com.manage.crm.webhook.domain.repository.WebhookRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class BrowseWebhookUseCaseTest : BehaviorSpec({
    lateinit var webhookRepository: WebhookRepository
    lateinit var browseWebhookUseCase: BrowseWebhookUseCase

    beforeTest {
        webhookRepository = mockk()
        browseWebhookUseCase = BrowseWebhookUseCase(webhookRepository)
    }

    afterTest { (_, _) ->
        clearMocks(webhookRepository)
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

                val response = browseWebhookUseCase.execute(BrowseWebhookUseCaseIn())

                response.webhooks.size shouldBe 2
                response.webhooks[0].id shouldBe webhook1.id!!
                response.webhooks[0].events shouldContainExactly listOf(WebhookEventType.USER_CREATED.value)
                response.webhooks[1].id shouldBe webhook2.id!!
                response.webhooks[1].events shouldContainExactly listOf(WebhookEventType.EMAIL_SENT.value)
                response.webhooks[1].active shouldBe false
            }
        }
    }
})
