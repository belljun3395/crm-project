
package com.manage.crm.user.controller

import com.manage.crm.integration.AbstractIntegrationTest
import com.manage.crm.user.controller.request.EnrollUserRequest
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Tag
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody

@Tag("integration")
class UserControllerIntegrationTest : AbstractIntegrationTest() {

    // Test Fixtures and Helper Methods
    private fun createTestUser(
        email: String = "test-${System.currentTimeMillis()}@example.com",
        name: String = "Test User",
        externalId: String = "test-user-${System.currentTimeMillis()}",
        age: String = "25"
    ) {
        val userAttributesJson = """
        {
            "email": "$email",
            "name": "$name",
            "age": "$age"
        }
        """.trimIndent()

        val request = EnrollUserRequest(
            id = null,
            externalId = externalId,
            userAttributes = userAttributesJson
        )

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
                // when & then
                createTestUser()
            }
        }

        describe("GET /api/v1/users/count") {
            it("get total user count") {
                // given - create some users
                val timestamp = System.currentTimeMillis()
                createTestUser(
                    email = "count-test1-$timestamp@example.com",
                    name = "Count Test User 1",
                    externalId = "count-test-user1-$timestamp",
                    age = "30"
                )
                createTestUser(
                    email = "count-test2-$timestamp@example.com",
                    name = "Count Test User 2",
                    externalId = "count-test-user2-$timestamp",
                    age = "28"
                )

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
