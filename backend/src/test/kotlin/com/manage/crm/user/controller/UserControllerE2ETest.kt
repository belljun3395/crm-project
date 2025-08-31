
package com.manage.crm.user.controller

import com.manage.crm.integration.AbstractE2ETest
import com.manage.crm.user.controller.request.EnrollUserRequest
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Tag
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody

@Tag("e2e")
class UserControllerE2ETest : AbstractE2ETest() {
    init {
        describe("GET /api/v1/users") {
            it("browse all users successfully") {
                // when & then
                webTestClient.get()
                    .uri("/api/v1/users")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody<String>()
                    .consumeWith { response ->
                        response.responseBody shouldNotBe null
                    }
            }
        }

        describe("POST /api/v1/users") {
            it("enroll user with valid data") {
                // given - email 필수 필드 포함
                val userAttributesJson = """
                {
                    "email": "test-${System.currentTimeMillis()}@example.com",
                    "name": "Test User",
                    "age": "25"
                }
                """.trimIndent()

                val request = EnrollUserRequest(
                    id = null,
                    externalId = "test-user-${System.currentTimeMillis()}",
                    userAttributes = userAttributesJson
                )

                // when & then
                webTestClient.post()
                    .uri("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody<String>()
                    .consumeWith { response ->
                        response.responseBody shouldNotBe null
                    }
            }
        }

        describe("GET /api/v1/users/count") {
            it("get total user count") {
                // when & then
                webTestClient.get()
                    .uri("/api/v1/users/count")
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
