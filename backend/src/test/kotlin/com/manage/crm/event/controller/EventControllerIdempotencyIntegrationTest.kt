package com.manage.crm.event.controller

import com.manage.crm.event.controller.request.PostCampaignPropertyDto
import com.manage.crm.event.controller.request.PostCampaignRequest
import com.manage.crm.integration.AbstractIntegrationTest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Tag
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.expectBody

@Tag("integration")
@TestPropertySource(properties = ["idempotency.enabled=true"])
class EventControllerIdempotencyIntegrationTest : AbstractIntegrationTest() {
    init {
        describe("POST /api/v1/events/campaign idempotency") {
            it("returns 400 when Idempotency-Key is missing") {
                val request = newCampaignRequest("missing-key")

                webTestClient.post()
                    .uri("/api/v1/events/campaign")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Idempotency-Key header is required")
            }

            it("replays completed response for same key and same body") {
                val key = "idem-campaign-same-${System.currentTimeMillis()}"
                val request = newCampaignRequest("same-body")

                val firstResponse = webTestClient.post()
                    .uri("/api/v1/events/campaign")
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody<String>()
                    .returnResult()
                    .responseBody!!

                val secondResponse = webTestClient.post()
                    .uri("/api/v1/events/campaign")
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody<String>()
                    .returnResult()
                    .responseBody!!

                firstResponse shouldBe secondResponse
            }

            it("returns 409 for same key with different body") {
                val key = "idem-campaign-diff-${System.currentTimeMillis()}"
                val firstRequest = newCampaignRequest("diff-a")
                val secondRequest = newCampaignRequest("diff-b")

                webTestClient.post()
                    .uri("/api/v1/events/campaign")
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(firstRequest)
                    .exchange()
                    .expectStatus().isCreated

                webTestClient.post()
                    .uri("/api/v1/events/campaign")
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(secondRequest)
                    .exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Idempotency-Key is already used with a different request body")
            }
        }
    }

    private fun newCampaignRequest(suffix: String): PostCampaignRequest {
        val timestamp = System.currentTimeMillis()
        return PostCampaignRequest(
            name = "idem-campaign-$suffix-$timestamp",
            properties = listOf(
                PostCampaignPropertyDto(key = "targetAudience", value = "segment-$suffix"),
                PostCampaignPropertyDto(key = "targetProduct", value = "product-$timestamp")
            )
        )
    }
}
