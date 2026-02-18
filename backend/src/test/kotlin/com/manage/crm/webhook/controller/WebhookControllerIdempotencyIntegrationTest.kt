package com.manage.crm.webhook.controller

import com.manage.crm.integration.AbstractIntegrationTest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Tag
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.expectBody

@Tag("integration")
@TestPropertySource(properties = ["webhook.enabled=true", "idempotency.enabled=true"])
class WebhookControllerIdempotencyIntegrationTest : AbstractIntegrationTest() {
    init {
        describe("Webhook idempotency") {
            it("returns 400 when Idempotency-Key is missing for update") {
                val createdId = createWebhook(
                    "missing-key-base",
                    "idem-webhook-create-${System.currentTimeMillis()}"
                )

                val updateJson = """
                    {
                      "name": "updated-${System.currentTimeMillis()}",
                      "url": "https://example.com/updated",
                      "events": ["USER_CREATED"],
                      "active": false
                    }
                """.trimIndent()

                webTestClient.put()
                    .uri("/api/v1/webhooks/$createdId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateJson)
                    .exchange()
                    .expectStatus().isBadRequest
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Idempotency-Key header is required")
            }

            it("replays completed response for same key and same update body") {
                val createdId = createWebhook(
                    "same-body-base",
                    "idem-webhook-create-${System.currentTimeMillis()}"
                )
                val key = "idem-webhook-update-same-${System.currentTimeMillis()}"
                val updateJson = """
                    {
                      "name": "updated-${System.currentTimeMillis()}",
                      "url": "https://example.com/updated",
                      "events": ["USER_CREATED", "EMAIL_SENT"],
                      "active": true
                    }
                """.trimIndent()

                val firstResponse = webTestClient.put()
                    .uri("/api/v1/webhooks/$createdId")
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateJson)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody<String>()
                    .returnResult()
                    .responseBody!!

                val secondResponse = webTestClient.put()
                    .uri("/api/v1/webhooks/$createdId")
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateJson)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody<String>()
                    .returnResult()
                    .responseBody!!

                firstResponse shouldBe secondResponse
            }
        }
    }

    private fun createWebhook(namePrefix: String, key: String): Long {
        val requestJson = """
            {
              "name": "$namePrefix-${System.currentTimeMillis()}",
              "url": "https://example.com/webhook",
              "events": ["USER_CREATED", "EMAIL_SENT"],
              "active": true
            }
        """.trimIndent()

        val responseBody = webTestClient.post()
            .uri("/api/v1/webhooks")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestJson)
            .exchange()
            .expectStatus().isCreated
            .expectBody<String>()
            .returnResult()
            .responseBody!!

        return objectMapper.readTree(responseBody)["data"]["id"].asLong()
    }
}
