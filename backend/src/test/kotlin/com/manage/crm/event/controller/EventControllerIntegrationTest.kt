
package com.manage.crm.event.controller

import com.manage.crm.event.controller.request.PostCampaignPropertyDto
import com.manage.crm.event.controller.request.PostCampaignRequest
import com.manage.crm.event.controller.request.PostEventPropertyDto
import com.manage.crm.event.controller.request.PostEventRequest
import com.manage.crm.integration.AbstractIntegrationTest
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Tag
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody

@Tag("integration")
class EventControllerIntegrationTest : AbstractIntegrationTest() {

    // Test Fixtures and Helper Methods
    private fun createTestUser(
        email: String = "test-user-${System.currentTimeMillis()}@example.com",
        name: String = "Test User",
        externalId: String = "test-user-${System.currentTimeMillis()}"
    ): String {
        val userAttributes = """
        {
            "email": "$email",
            "name": "$name"
        }
        """.trimIndent()

        val userRequest = mapOf(
            "externalId" to externalId,
            "userAttributes" to userAttributes
        )

        webTestClient.post()
            .uri("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(userRequest)
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
        val campaignRequest = PostCampaignRequest(
            name = name,
            properties = properties
        )

        webTestClient.post()
            .uri("/api/v1/events/campaign")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(campaignRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody<String>()
            .consumeWith { response ->
                response.responseBody shouldNotBe null
            }

        return campaignRequest.name
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
        val eventRequest = PostEventRequest(
            name = name,
            campaignName = campaignName,
            externalId = userExternalId,
            properties = properties
        )

        webTestClient.post()
            .uri("/api/v1/events")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(eventRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody<String>()
            .consumeWith { response ->
                response.responseBody shouldNotBe null
            }
    }

    private fun searchEvents(
        eventName: String,
        searchConditions: String
    ) {
        webTestClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/api/v1/events")
                    .queryParam("eventName", eventName)
                    .queryParam("where", searchConditions)
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .consumeWith { response ->
                response.responseBody shouldNotBe null
            }
    }
    init {
        describe("POST /api/v1/events") {
            it("create event successfully without campaign") {
                // given
                val userExternalId = createTestUser(
                    email = "test-event-${System.currentTimeMillis()}@example.com",
                    name = "Event Test User"
                )

                // when & then
                createTestEvent(
                    name = "purchase_event",
                    campaignName = null,
                    userExternalId = userExternalId
                )
            }

            it("create event successfully with campaign") {
                // given
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

                // when & then
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
                // when & then
                createTestCampaign(
                    name = "simple_campaign_${System.currentTimeMillis()}",
                    properties = listOf(
                        PostCampaignPropertyDto(key = "campaign_type", value = "discount"),
                        PostCampaignPropertyDto(key = "discount_rate", value = "20")
                    )
                )
            }

            it("create campaign without properties") {
                // when & then
                createTestCampaign(
                    name = "no_property_campaign_${System.currentTimeMillis()}",
                    properties = emptyList()
                )
            }
        }

        describe("GET /api/v1/events") {
            it("search events with single property condition") {
                // given
                val timestamp = System.currentTimeMillis()
                val userExternalId = createTestUser(
                    email = "search-test-$timestamp@example.com",
                    name = "Search Test User",
                    externalId = "search-user-$timestamp"
                )

                createTestEvent(
                    name = "view_product",
                    campaignName = null,
                    userExternalId = userExternalId,
                    properties = listOf(
                        PostEventPropertyDto(key = "category", value = "electronics"),
                        PostEventPropertyDto(key = "brand", value = "samsung")
                    )
                )

                // when & then
                searchEvents(
                    eventName = "view_product",
                    searchConditions = "category&electronics&=&end"
                )
            }

            it("search events with multiple property conditions") {
                // given
                val timestamp = System.currentTimeMillis()
                val userExternalId = createTestUser(
                    email = "multi-search-test-$timestamp@example.com",
                    name = "Multi Search Test User",
                    externalId = "multi-search-user-$timestamp"
                )

                createTestEvent(
                    name = "purchase_completed",
                    campaignName = null,
                    userExternalId = userExternalId,
                    properties = listOf(
                        PostEventPropertyDto(key = "action", value = "purchase"),
                        PostEventPropertyDto(key = "amount", value = "150"),
                        PostEventPropertyDto(key = "currency", value = "USD")
                    )
                )

                // when & then
                searchEvents(
                    eventName = "purchase_completed",
                    searchConditions = "action&purchase&=&and,amount&150&=&end"
                )
            }

            it("returns 400 when where format is invalid") {
                // given
                val timestamp = System.currentTimeMillis()
                val userExternalId = createTestUser(
                    email = "invalid-format-$timestamp@example.com",
                    name = "Invalid Format User",
                    externalId = "invalid-format-user-$timestamp"
                )

                createTestEvent(
                    name = "invalid_where_event",
                    userExternalId = userExternalId
                )

                // when & then
                webTestClient.get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/api/v1/events")
                            .queryParam("eventName", "invalid_where_event")
                            .queryParam("where", "category&electronics&=")
                            .build()
                    }
                    .exchange()
                    .expectStatus().isBadRequest
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Invalid where format at index 0")
            }

            it("returns 400 when where contains invalid operation") {
                // given
                val timestamp = System.currentTimeMillis()
                val userExternalId = createTestUser(
                    email = "invalid-op-$timestamp@example.com",
                    name = "Invalid Operation User",
                    externalId = "invalid-op-user-$timestamp"
                )

                createTestEvent(
                    name = "invalid_operation_event",
                    userExternalId = userExternalId
                )

                // when & then
                webTestClient.get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/api/v1/events")
                            .queryParam("eventName", "invalid_operation_event")
                            .queryParam("where", "category&electronics&LIKEX&end")
                            .build()
                    }
                    .exchange()
                    .expectStatus().isBadRequest
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Invalid operation at index 0: LIKEX")
            }

            it("returns 400 when BETWEEN keys are different") {
                // given
                val timestamp = System.currentTimeMillis()
                val userExternalId = createTestUser(
                    email = "between-key-$timestamp@example.com",
                    name = "Between Key User",
                    externalId = "between-key-user-$timestamp"
                )

                createTestEvent(
                    name = "between_key_event",
                    userExternalId = userExternalId,
                    properties = listOf(
                        PostEventPropertyDto(key = "amount", value = "100"),
                        PostEventPropertyDto(key = "amount", value = "200")
                    )
                )

                // when & then
                webTestClient.get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/api/v1/events")
                            .queryParam("eventName", "between_key_event")
                            .queryParam("where", "startAmount&100&endAmount&200&BETWEEN&end")
                            .build()
                    }
                    .exchange()
                    .expectStatus().isBadRequest
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Between operation requires the same key at index 0")
            }
        }
    }
}
