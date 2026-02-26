package com.manage.crm.webhook.application

import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.webhook.application.dto.BrowseWebhookDeadLettersUseCaseIn
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookDeadLetter
import com.manage.crm.webhook.domain.WebhookEventType
import com.manage.crm.webhook.domain.WebhookEvents
import com.manage.crm.webhook.domain.repository.WebhookDeadLetterRepository
import com.manage.crm.webhook.domain.repository.WebhookRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class BrowseWebhookDeadLettersUseCaseTest : BehaviorSpec({
    lateinit var webhookRepository: WebhookRepository
    lateinit var webhookDeadLetterRepository: WebhookDeadLetterRepository
    lateinit var useCase: BrowseWebhookDeadLettersUseCase

    beforeTest {
        webhookRepository = mockk()
        webhookDeadLetterRepository = mockk()
        useCase = BrowseWebhookDeadLettersUseCase(
            webhookRepository = webhookRepository,
            webhookDeadLetterRepository = webhookDeadLetterRepository
        )
    }

    given("browse webhook dead letters") {
        `when`("webhook does not exist") {
            then("throw not found") {
                val webhookId = 999L
                coEvery { webhookRepository.findById(webhookId) } returns null

                shouldThrow<NotFoundByIdException> {
                    useCase.execute(BrowseWebhookDeadLettersUseCaseIn(webhookId = webhookId, limit = 20))
                }
            }
        }

        `when`("dead letters exist") {
            then("return dead letters in descending createdAt order") {
                val webhookId = 1L
                val webhook = Webhook.new(
                    id = webhookId,
                    name = "primary",
                    url = "https://example.com/webhook",
                    events = WebhookEvents(listOf(WebhookEventType.USER_CREATED)),
                    active = true,
                    createdAt = LocalDateTime.of(2024, 1, 1, 0, 0)
                )
                val latest = WebhookDeadLetter.new(
                    webhookId = webhookId,
                    eventId = "event-2",
                    eventType = WebhookEventType.USER_CREATED.value,
                    payloadJson = "{\"eventId\":\"event-2\"}",
                    deliveryStatus = "BLOCKED",
                    attemptCount = 1,
                    responseStatus = null,
                    errorMessage = "rate limit"
                ).apply {
                    id = 2L
                    createdAt = LocalDateTime.of(2024, 1, 2, 0, 0)
                }
                val older = WebhookDeadLetter.new(
                    webhookId = webhookId,
                    eventId = "event-1",
                    eventType = WebhookEventType.USER_CREATED.value,
                    payloadJson = "{\"eventId\":\"event-1\"}",
                    deliveryStatus = "FAILED",
                    attemptCount = 3,
                    responseStatus = 500,
                    errorMessage = "timeout"
                ).apply {
                    id = 1L
                    createdAt = LocalDateTime.of(2024, 1, 1, 12, 0)
                }

                coEvery { webhookRepository.findById(webhookId) } returns webhook
                every { webhookDeadLetterRepository.findByWebhookIdOrderByCreatedAtDesc(webhookId) } returns flowOf(latest, older)

                val result = useCase.execute(BrowseWebhookDeadLettersUseCaseIn(webhookId = webhookId, limit = 10))

                result.deadLetters.size shouldBe 2
                result.deadLetters[0].eventId shouldBe "event-2"
                result.deadLetters[0].deliveryStatus shouldBe "BLOCKED"
                result.deadLetters[1].eventId shouldBe "event-1"
                result.deadLetters[1].deliveryStatus shouldBe "FAILED"
            }
        }
    }
})
