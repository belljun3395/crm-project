package com.manage.crm.webhook.application

import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.webhook.application.dto.BrowseWebhookDeliveryLogsUseCaseIn
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookDeliveryLog
import com.manage.crm.webhook.domain.WebhookEventType
import com.manage.crm.webhook.domain.WebhookEvents
import com.manage.crm.webhook.domain.repository.WebhookDeliveryLogRepository
import com.manage.crm.webhook.domain.repository.WebhookRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class BrowseWebhookDeliveryLogsUseCaseTest : BehaviorSpec({
    lateinit var webhookRepository: WebhookRepository
    lateinit var webhookDeliveryLogRepository: WebhookDeliveryLogRepository
    lateinit var useCase: BrowseWebhookDeliveryLogsUseCase

    beforeTest {
        webhookRepository = mockk()
        webhookDeliveryLogRepository = mockk()
        useCase = BrowseWebhookDeliveryLogsUseCase(
            webhookRepository = webhookRepository,
            webhookDeliveryLogRepository = webhookDeliveryLogRepository
        )
    }

    given("browse webhook deliveries") {
        `when`("webhook does not exist") {
            then("throw not found") {
                val webhookId = 999L
                coEvery { webhookRepository.findById(webhookId) } returns null

                shouldThrow<NotFoundByIdException> {
                    useCase.execute(BrowseWebhookDeliveryLogsUseCaseIn(webhookId = webhookId, limit = 20))
                }
            }
        }

        `when`("delivery logs exist") {
            then("return logs in descending deliveredAt order") {
                val webhookId = 1L
                val webhook = Webhook.new(
                    id = webhookId,
                    name = "primary",
                    url = "https://example.com/webhook",
                    events = WebhookEvents(listOf(WebhookEventType.USER_CREATED)),
                    active = true,
                    createdAt = LocalDateTime.of(2024, 1, 1, 0, 0)
                )
                val latest = WebhookDeliveryLog.new(
                    webhookId = webhookId,
                    eventId = "event-2",
                    eventType = WebhookEventType.USER_CREATED.value,
                    deliveryStatus = "SUCCESS",
                    attemptCount = 1,
                    responseStatus = 200,
                    errorMessage = null
                ).apply {
                    id = 2L
                    deliveredAt = LocalDateTime.of(2024, 1, 2, 0, 0)
                }
                val older = WebhookDeliveryLog.new(
                    webhookId = webhookId,
                    eventId = "event-1",
                    eventType = WebhookEventType.USER_CREATED.value,
                    deliveryStatus = "FAILED",
                    attemptCount = 3,
                    responseStatus = 500,
                    errorMessage = "timeout"
                ).apply {
                    id = 1L
                    deliveredAt = LocalDateTime.of(2024, 1, 1, 12, 0)
                }

                coEvery { webhookRepository.findById(webhookId) } returns webhook
                every { webhookDeliveryLogRepository.findByWebhookIdOrderByDeliveredAtDesc(webhookId) } returns flowOf(latest, older)

                val result = useCase.execute(BrowseWebhookDeliveryLogsUseCaseIn(webhookId = webhookId, limit = 10))

                result.deliveries.size shouldBe 2
                result.deliveries[0].eventId shouldBe "event-2"
                result.deliveries[0].deliveryStatus shouldBe "SUCCESS"
                result.deliveries[1].eventId shouldBe "event-1"
                result.deliveries[1].deliveryStatus shouldBe "FAILED"
            }
        }
    }
})
