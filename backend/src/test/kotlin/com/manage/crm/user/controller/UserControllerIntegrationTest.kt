
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

            it("browse users with pagination parameters") {
                // given - create test users
                val timestamp = System.currentTimeMillis()
                repeat(5) { index ->
                    createTestUser(
                        email = "pagination-test$index-$timestamp@example.com",
                        name = "Pagination Test User $index",
                        externalId = "pagination-test-user$index-$timestamp",
                        age = "${20 + index}"
                    )
                }

                // when & then - first page with size=2
                webTestClient.get()
                    .uri { builder ->
                        builder.path("/api/v1/users")
                            .queryParam("page", 0)
                            .queryParam("size", 2)
                            .build()
                    }
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.data.users.page").isEqualTo(0)
                    .jsonPath("$.data.users.size").isEqualTo(2)
                    .jsonPath("$.data.users.content").isArray
                    .jsonPath("$.data.users.totalElements").isNumber
                    .jsonPath("$.data.users.totalPages").isNumber

                // when & then - second page with size=2
                webTestClient.get()
                    .uri { builder ->
                        builder.path("/api/v1/users")
                            .queryParam("page", 1)
                            .queryParam("size", 2)
                            .build()
                    }
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.data.users.page").isEqualTo(1)
                    .jsonPath("$.data.users.size").isEqualTo(2)
                    .jsonPath("$.data.users.content").isArray
                    .jsonPath("$.data.users.totalElements").isNumber
                    .jsonPath("$.data.users.totalPages").isNumber
            }

            it("browse users with default pagination parameters") {
                // when & then - using default values (page=0, size=20)
                webTestClient.get()
                    .uri("/api/v1/users")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody()
                    .jsonPath("$.data.users.page").isEqualTo(0)
                    .jsonPath("$.data.users.size").isEqualTo(20)
                    .jsonPath("$.data.users.content").isArray
                    .jsonPath("$.data.users.totalElements").isNumber
                    .jsonPath("$.data.users.totalPages").isNumber
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
