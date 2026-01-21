package com.manage.crm.webhook.domain

import com.manage.crm.integration.AbstractIntegrationTest
import com.manage.crm.webhook.domain.repository.WebhookRepository
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Tag
import org.springframework.test.context.TestPropertySource

@Tag("integration")
@TestPropertySource(properties = ["webhook.enabled=true"])
class WebhookRepositoryIntegrationTest(
    private val webhookRepository: WebhookRepository
) : AbstractIntegrationTest() {
    init {
        describe("Webhook repository mapping") {
            it("persists and fetches events JSON and active flag") {
                val webhook = Webhook.new(
                    name = "repo-${System.currentTimeMillis()}",
                    url = "https://example.com/repo",
                    events = WebhookEvents(listOf(WebhookEventType.USER_CREATED, WebhookEventType.EMAIL_SENT)),
                    active = true
                )

                val saved = webhookRepository.save(webhook)

                val found = webhookRepository.findById(requireNotNull(saved.id))
                requireNotNull(found).events.toValues() shouldContainExactly listOf(
                    WebhookEventType.USER_CREATED.value,
                    WebhookEventType.EMAIL_SENT.value
                )
                found.active shouldBe true

                val activeByEvent = webhookRepository.findActiveByEvent(WebhookEventType.EMAIL_SENT.value)
                activeByEvent.map { it.id } shouldContain saved.id
            }
        }
    }
}
