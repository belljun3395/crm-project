package com.manage.crm.webhook.controller

import com.manage.crm.integration.AbstractIntegrationTest
import org.junit.jupiter.api.Tag
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource

@Tag("integration")
@TestPropertySource(properties = ["webhook.enabled=true", "message.provider=kafka", "scheduler.provider=redis-kafka", "mail.provider=javamail"])
class WebhookControllerIntegrationTest : AbstractIntegrationTest() {
    init {
        describe("Webhook API") {
            it("creates, reads, and lists webhooks") {
                val uniqueName = "webhook-${System.currentTimeMillis()}"
                val requestJson = """
                    {
                      "name": "$uniqueName",
                      "url": "https://example.com/webhook",
                      "events": ["USER_CREATED", "EMAIL_SENT"],
                      "active": true
                    }
                """.trimIndent()

                val createResponse = webTestClient.post()
                    .uri("/api/v1/webhooks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestJson)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody()
                    .jsonPath("$.data.id").isNumber
                    .jsonPath("$.data.name").isEqualTo(uniqueName)
                    .jsonPath("$.data.url").isEqualTo("https://example.com/webhook")
                    .jsonPath("$.data.events[0]").isEqualTo("USER_CREATED")
                    .jsonPath("$.data.events[1]").isEqualTo("EMAIL_SENT")
                    .returnResult()

                val createdId = objectMapper.readTree(createResponse.responseBody).get("data").get("id").asLong()

                webTestClient.get()
                    .uri("/api/v1/webhooks/$createdId")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.data.id").isEqualTo(createdId)
                    .jsonPath("$.data.name").isEqualTo(uniqueName)
                    .jsonPath("$.data.active").isEqualTo(true)

                webTestClient.get()
                    .uri("/api/v1/webhooks")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.data.length()").isNumber
                    .jsonPath("$.data[?(@.id==$createdId)]").exists()
            }
        }
    }
}
