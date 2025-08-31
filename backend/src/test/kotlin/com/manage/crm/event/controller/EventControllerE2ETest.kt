
package com.manage.crm.event.controller

import com.manage.crm.event.controller.request.PostCampaignPropertyDto
import com.manage.crm.event.controller.request.PostCampaignRequest
import com.manage.crm.event.controller.request.PostEventPropertyDto
import com.manage.crm.event.controller.request.PostEventRequest
import com.manage.crm.integration.AbstractE2ETest
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Tag
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody

@Tag("e2e")
class EventControllerE2ETest : AbstractE2ETest() {
    init {
        describe("POST /api/v1/events") {
            it("create event successfully without campaign") {
                // given - create user first to ensure valid externalId
                val userAttributes = """
                {
                    "email": "test-event-${System.currentTimeMillis()}@example.com",
                    "name": "Event Test User"
                }
                """.trimIndent()

                val userRequest = mapOf(
                    "externalId" to "event-user-${System.currentTimeMillis()}",
                    "userAttributes" to userAttributes
                )

                webTestClient.post()
                    .uri("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userRequest)
                    .exchange()
                    .expectStatus().isOk

                // given - simple event without campaign
                val properties = listOf(
                    PostEventPropertyDto(key = "product_id", value = "12345"),
                    PostEventPropertyDto(key = "price", value = "99.99")
                )

                val request = PostEventRequest(
                    name = "purchase_event",
                    campaignName = null, // no campaign
                    externalId = userRequest["externalId"] as String,
                    properties = properties
                )

                // when & then
                webTestClient.post()
                    .uri("/api/v1/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody<String>()
                    .consumeWith { response ->
                        response.responseBody shouldNotBe null
                    }
            }

            it("create event successfully with campaign") {
                // given - create campaign first
                val campaignProperties = listOf(
                    PostCampaignPropertyDto(key = "eventCount", value = "10"),
                    PostCampaignPropertyDto(key = "targetAudience", value = "new_users")
                )

                val campaignRequest = PostCampaignRequest(
                    name = "test_campaign_${System.currentTimeMillis()}",
                    properties = campaignProperties
                )

                webTestClient.post()
                    .uri("/api/v1/events/campaign")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(campaignRequest)
                    .exchange()
                    .expectStatus().isCreated

                // given - create user
                val userAttributes = """
                {
                    "email": "test-event-with-campaign-${System.currentTimeMillis()}@example.com",
                    "name": "Event Campaign Test User"
                }
                """.trimIndent()

                val userRequest = mapOf(
                    "externalId" to "event-campaign-user-${System.currentTimeMillis()}",
                    "userAttributes" to userAttributes
                )

                webTestClient.post()
                    .uri("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userRequest)
                    .exchange()
                    .expectStatus().isOk

                // given - event with campaign
                val eventProperties = listOf(
                    PostEventPropertyDto(key = "action", value = "signup"),
                    PostEventPropertyDto(key = "source", value = "web")
                )

                val eventRequest = PostEventRequest(
                    name = "user_signup_with_campaign",
                    campaignName = campaignRequest.name, // link to campaign
                    externalId = userRequest["externalId"] as String,
                    properties = eventProperties
                )

                // when & then
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
        }

        describe("POST /api/v1/events/campaign") {
            it("create campaign with simple properties") {
                // given - simple campaign with basic properties
                val properties = listOf(
                    PostCampaignPropertyDto(key = "campaign_type", value = "discount"),
                    PostCampaignPropertyDto(key = "discount_rate", value = "20")
                )

                val request = PostCampaignRequest(
                    name = "simple_campaign_${System.currentTimeMillis()}",
                    properties = properties
                )

                // when & then
                webTestClient.post()
                    .uri("/api/v1/events/campaign")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody<String>()
                    .consumeWith { response ->
                        response.responseBody shouldNotBe null
                    }
            }
        }

        describe("GET /api/v1/events") {
            it("search events with simple conditions") {
                // given - create user first for event
                val userAttributes = """
                {
                    "email": "search-test-${System.currentTimeMillis()}@example.com",
                    "name": "Search Test User"
                }
                """.trimIndent()

                val userRequest = mapOf(
                    "externalId" to "search-user-${System.currentTimeMillis()}",
                    "userAttributes" to userAttributes
                )

                webTestClient.post()
                    .uri("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userRequest)
                    .exchange()
                    .expectStatus().isOk

                // given - create event to search
                val eventProperties = listOf(
                    PostEventPropertyDto(key = "product_category", value = "electronics"),
                    PostEventPropertyDto(key = "price_range", value = "100-200")
                )

                val eventRequest = PostEventRequest(
                    name = "product_view",
                    campaignName = null,
                    externalId = userRequest["externalId"] as String,
                    properties = eventProperties
                )

                webTestClient.post()
                    .uri("/api/v1/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(eventRequest)
                    .exchange()
                    .expectStatus().isCreated

                // when & then - search events with simple condition
                val searchConditions = "product_category&electronics&=&end"
                webTestClient.get()
                    .uri("/api/v1/events?eventName=product_view&where=$searchConditions")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody<String>()
                    .consumeWith { response ->
                        response.responseBody shouldNotBe null
                    }
            }
        }
    }
}
