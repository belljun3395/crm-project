package com.manage.crm.journey.controller

import com.manage.crm.integration.AbstractIntegrationTest
import org.junit.jupiter.api.Tag
import org.springframework.http.MediaType

@Tag("integration")
class JourneyControllerIntegrationTest : AbstractIntegrationTest() {
    private fun createJourney(name: String = "journey-${System.currentTimeMillis()}"): Long {
        val requestJson =
            """
            {
              "name": "$name",
              "triggerType": "EVENT",
              "triggerEventName": "purchase",
              "active": true,
              "steps": [
                {
                  "stepOrder": 1,
                  "stepType": "ACTION",
                  "channel": "SLACK",
                  "destination": "https://hooks.slack.com/services/T000/B000/XXX",
                  "subject": "welcome",
                  "body": "hello",
                  "variables": {},
                  "retryCount": 0
                }
              ]
            }
            """.trimIndent()

        val response =
            webTestClient
                .post()
                .uri("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .exchange()
                .expectStatus()
                .isCreated
                .expectBody()
                .jsonPath("$.data.id")
                .isNumber
                .returnResult()

        return objectMapper
            .readTree(response.responseBody)
            .get("data")
            .get("id")
            .asLong()
    }

    init {
        describe("Journey API") {
            it("creates and updates journey") {
                val journeyId = createJourney()

                val updateJson =
                    """
                    {
                      "name": "updated-journey-$journeyId",
                      "triggerType": "EVENT",
                      "triggerEventName": "purchase",
                      "active": true,
                      "steps": [
                        {
                          "stepOrder": 1,
                          "stepType": "ACTION",
                          "channel": "SLACK",
                          "destination": "https://hooks.slack.com/services/T000/B000/YYY",
                          "subject": "notice",
                          "body": "updated",
                          "variables": {},
                          "retryCount": 0
                        }
                      ]
                    }
                    """.trimIndent()

                webTestClient
                    .put()
                    .uri("/api/v1/journeys/$journeyId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateJson)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .jsonPath("$.data.id")
                    .isEqualTo(journeyId)
                    .jsonPath("$.data.name")
                    .isEqualTo("updated-journey-$journeyId")
            }

            it("applies lifecycle transitions and blocks archived resume") {
                val journeyId = createJourney("lifecycle-${System.currentTimeMillis()}")

                webTestClient
                    .post()
                    .uri("/api/v1/journeys/$journeyId/pause")
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .jsonPath("$.data.lifecycleStatus")
                    .isEqualTo("PAUSED")

                webTestClient
                    .post()
                    .uri("/api/v1/journeys/$journeyId/resume")
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .jsonPath("$.data.lifecycleStatus")
                    .isEqualTo("ACTIVE")

                webTestClient
                    .post()
                    .uri("/api/v1/journeys/$journeyId/archive")
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody()
                    .jsonPath("$.data.lifecycleStatus")
                    .isEqualTo("ARCHIVED")

                webTestClient
                    .post()
                    .uri("/api/v1/journeys/$journeyId/resume")
                    .exchange()
                    .expectStatus()
                    .isBadRequest
            }

            it("returns bad request for invalid trigger type") {
                val invalidJson =
                    """
                    {
                      "name": "invalid-journey",
                      "triggerType": "INVALID",
                      "triggerEventName": "purchase",
                      "active": true,
                      "steps": [
                        {
                          "stepOrder": 1,
                          "stepType": "ACTION",
                          "channel": "SLACK",
                          "destination": "https://hooks.slack.com/services/T000/B000/XXX",
                          "body": "hello",
                          "variables": {},
                          "retryCount": 0
                        }
                      ]
                    }
                    """.trimIndent()

                webTestClient
                    .post()
                    .uri("/api/v1/journeys")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invalidJson)
                    .exchange()
                    .expectStatus()
                    .isBadRequest
            }
        }
    }
}
