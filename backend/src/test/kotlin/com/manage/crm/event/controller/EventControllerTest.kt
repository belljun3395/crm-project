package com.manage.crm.event.controller

import com.manage.crm.event.controller.request.PostCampaignPropertyDto
import com.manage.crm.event.controller.request.PostCampaignRequest
import com.manage.crm.event.controller.request.PostEventPropertyDto
import com.manage.crm.event.controller.request.PostEventRequest
import com.manage.crm.integration.AbstractIntegrationTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Tag
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.expectBody

@Tag("integration")
class EventControllerTest : AbstractIntegrationTest() {

    private fun createTestUser(
        email: String = "test-user-${System.currentTimeMillis()}@example.com",
        name: String = "Test User",
        externalId: String = "test-user-${System.currentTimeMillis()}"
    ): String {
        webTestClient.post()
            .uri("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("externalId" to externalId, "userAttributes" to """{"email":"$email","name":"$name"}"""))
            .exchange()
            .expectStatus().isOk
        return externalId
    }

    private fun createTestCampaign(
        name: String = "test-campaign-${System.currentTimeMillis()}",
        properties: List<PostCampaignPropertyDto> = listOf(
            PostCampaignPropertyDto(key = "targetProduct", value = "premium_product"),
            PostCampaignPropertyDto(key = "targetAudience", value = "premium_users")
        )
    ): String {
        webTestClient.post()
            .uri("/api/v1/events/campaign")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(PostCampaignRequest(name = name, properties = properties))
            .exchange()
            .expectStatus().isCreated
            .expectBody<String>()
            .consumeWith { it.responseBody shouldNotBe null }
        return name
    }

    private fun createTestEvent(
        name: String,
        campaignName: String? = null,
        userExternalId: String,
        properties: List<PostEventPropertyDto> = listOf(
            PostEventPropertyDto(key = "product_id", value = "12345"),
            PostEventPropertyDto(key = "price", value = "99.99")
        )
    ) {
        webTestClient.post()
            .uri("/api/v1/events")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(PostEventRequest(name = name, campaignName = campaignName, externalId = userExternalId, properties = properties))
            .exchange()
            .expectStatus().isCreated
            .expectBody<String>()
            .consumeWith { it.responseBody shouldNotBe null }
    }

    init {
        describe("EventController") {
            describe("POST /api/v1/events") {
                it("create event successfully without campaign") {
                    val userExternalId = createTestUser(
                        email = "test-event-${System.currentTimeMillis()}@example.com",
                        name = "Event Test User"
                    )
                    createTestEvent(name = "purchase_event", campaignName = null, userExternalId = userExternalId)
                }

                it("create event successfully with campaign") {
                    val campaignName = createTestCampaign(
                        properties = listOf(
                            PostCampaignPropertyDto(key = "targetProduct", value = "10"),
                            PostCampaignPropertyDto(key = "targetAudience", value = "new_users")
                        )
                    )
                    val userExternalId = createTestUser(
                        email = "test-event-with-campaign-${System.currentTimeMillis()}@example.com",
                        name = "Event Campaign Test User"
                    )
                    createTestEvent(
                        name = "user_signup_with_campaign",
                        campaignName = campaignName,
                        userExternalId = userExternalId,
                        properties = listOf(
                            PostEventPropertyDto(key = "targetProduct", value = "product_10"),
                            PostEventPropertyDto(key = "targetAudience", value = "new_users")
                        )
                    )
                }
            }

            describe("POST /api/v1/events/campaign") {
                it("create campaign with properties") {
                    createTestCampaign(
                        name = "simple_campaign_${System.currentTimeMillis()}",
                        properties = listOf(
                            PostCampaignPropertyDto(key = "campaign_type", value = "discount"),
                            PostCampaignPropertyDto(key = "discount_rate", value = "20")
                        )
                    )
                }

                it("create campaign without properties") {
                    createTestCampaign(
                        name = "no_property_campaign_${System.currentTimeMillis()}",
                        properties = emptyList()
                    )
                }
            }

            describe("GET /api/v1/events") {
                it("search events with single property condition") {
                    val timestamp = System.currentTimeMillis()
                    val userExternalId = createTestUser(
                        email = "search-test-$timestamp@example.com",
                        externalId = "search-user-$timestamp"
                    )
                    createTestEvent(
                        name = "view_product",
                        userExternalId = userExternalId,
                        properties = listOf(
                            PostEventPropertyDto(key = "category", value = "electronics"),
                            PostEventPropertyDto(key = "brand", value = "samsung")
                        )
                    )
                    webTestClient.get()
                        .uri { it.path("/api/v1/events").queryParam("eventName", "view_product").queryParam("where", "category&electronics&=&end").build() }
                        .exchange()
                        .expectStatus().isOk
                        .expectBody<String>()
                        .consumeWith { it.responseBody shouldNotBe null }
                }

                it("search events with multiple property conditions") {
                    val timestamp = System.currentTimeMillis()
                    val userExternalId = createTestUser(
                        email = "multi-search-test-$timestamp@example.com",
                        externalId = "multi-search-user-$timestamp"
                    )
                    createTestEvent(
                        name = "purchase_completed",
                        userExternalId = userExternalId,
                        properties = listOf(
                            PostEventPropertyDto(key = "action", value = "purchase"),
                            PostEventPropertyDto(key = "amount", value = "150"),
                            PostEventPropertyDto(key = "currency", value = "USD")
                        )
                    )
                    webTestClient.get()
                        .uri { it.path("/api/v1/events").queryParam("eventName", "purchase_completed").queryParam("where", "action&purchase&=&and,amount&150&=&end").build() }
                        .exchange()
                        .expectStatus().isOk
                }

                it("returns 400 when where format is invalid") {
                    webTestClient.get()
                        .uri { it.path("/api/v1/events").queryParam("eventName", "any_event").queryParam("where", "category&electronics&=").build() }
                        .exchange()
                        .expectStatus().isBadRequest
                        .expectBody()
                        .jsonPath("$.message").isEqualTo("Invalid where format at index 0")
                }

                it("returns 400 when where contains invalid operation") {
                    webTestClient.get()
                        .uri { it.path("/api/v1/events").queryParam("eventName", "any_event").queryParam("where", "category&electronics&LIKEX&end").build() }
                        .exchange()
                        .expectStatus().isBadRequest
                        .expectBody()
                        .jsonPath("$.message").isEqualTo("Invalid operation at index 0: LIKEX")
                }

                it("returns 400 when BETWEEN keys are different") {
                    webTestClient.get()
                        .uri { it.path("/api/v1/events").queryParam("eventName", "any_event").queryParam("where", "startAmount&100&endAmount&200&BETWEEN&end").build() }
                        .exchange()
                        .expectStatus().isBadRequest
                        .expectBody()
                        .jsonPath("$.message").isEqualTo("Between operation requires the same key at index 0")
                }
            }
        }
    }
}

@Tag("integration")
@TestPropertySource(properties = ["idempotency.enabled=true"])
class EventControllerIdempotencyTest : AbstractIntegrationTest() {

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

    init {
        describe("EventController idempotency") {
            describe("POST /api/v1/events/campaign") {
                it("returns 400 when Idempotency-Key is missing") {
                    webTestClient.post()
                        .uri("/api/v1/events/campaign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(newCampaignRequest("missing-key"))
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
                        .expectBody<String>().returnResult().responseBody!!

                    val secondResponse = webTestClient.post()
                        .uri("/api/v1/events/campaign")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .exchange()
                        .expectStatus().isCreated
                        .expectBody<String>().returnResult().responseBody!!

                    firstResponse shouldBe secondResponse
                }

                it("returns 409 for same key with different body") {
                    val key = "idem-campaign-diff-${System.currentTimeMillis()}"

                    webTestClient.post()
                        .uri("/api/v1/events/campaign")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(newCampaignRequest("diff-a"))
                        .exchange()
                        .expectStatus().isCreated

                    webTestClient.post()
                        .uri("/api/v1/events/campaign")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(newCampaignRequest("diff-b"))
                        .exchange()
                        .expectStatus().isEqualTo(409)
                        .expectBody()
                        .jsonPath("$.message").isEqualTo("Idempotency-Key is already used with a different request body")
                }
            }
        }
    }
}
