package com.manage.crm.user.controller

import com.manage.crm.integration.AbstractIntegrationTest
import com.manage.crm.user.controller.request.EnrollUserRequest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Tag
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource

@Tag("integration")
@TestPropertySource(properties = ["idempotency.enabled=true"])
class UserControllerIdempotencyIntegrationTest : AbstractIntegrationTest() {
    init {
        describe("POST /api/v1/users idempotency") {
            it("returns 400 when Idempotency-Key is missing") {
                val request = newEnrollUserRequest("missing-key")

                webTestClient.post()
                    .uri("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Idempotency-Key header is required")
            }

            it("replays completed response for same key and same body") {
                val key = "idem-user-same-body-001"
                val request = newEnrollUserRequest("same-body")

                val firstResponse = webTestClient.post()
                    .uri("/api/v1/users")
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody<String>()
                    .returnResult()
                    .responseBody!!

                val secondResponse = webTestClient.post()
                    .uri("/api/v1/users")
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody<String>()
                    .returnResult()
                    .responseBody!!

                firstResponse shouldBe secondResponse
            }

            it("returns 409 when same key is used with different body") {
                val key = "idem-user-diff-body-001"
                val firstRequest = newEnrollUserRequest("diff-a")
                val secondRequest = newEnrollUserRequest("diff-b")

                webTestClient.post()
                    .uri("/api/v1/users")
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(firstRequest)
                    .exchange()
                    .expectStatus().isOk

                webTestClient.post()
                    .uri("/api/v1/users")
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

    private fun newEnrollUserRequest(suffix: String): EnrollUserRequest {
        val timestamp = System.currentTimeMillis()
        return EnrollUserRequest(
            id = null,
            externalId = "idempotency-user-$suffix-$timestamp",
            userAttributes = """
            {
                "email": "idempotency-$suffix-$timestamp@example.com",
                "name": "Idempotency User",
                "age": "29"
            }
            """.trimIndent()
        )
    }
}
