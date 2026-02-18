package com.manage.crm.email.controller

import com.manage.crm.email.controller.request.PostTemplateRequest
import com.manage.crm.email.controller.request.SendNotificationEmailRequest
import com.manage.crm.integration.AbstractIntegrationTest
import com.manage.crm.user.controller.request.EnrollUserRequest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Tag
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.expectBody

@Tag("integration")
@TestPropertySource(properties = ["idempotency.enabled=true"])
class EmailControllerIdempotencyIntegrationTest : AbstractIntegrationTest() {
    init {
        describe("POST /api/v1/emails/send/notifications idempotency") {
            it("returns 400 when Idempotency-Key is missing") {
                val templateId = createTemplate()
                val userId = createUserWithIdempotencyHeader("missing-key-user")
                val request = SendNotificationEmailRequest(
                    campaignId = null,
                    templateId = templateId,
                    templateVersion = null,
                    userIds = listOf(userId)
                )

                webTestClient.post()
                    .uri("/api/v1/emails/send/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Idempotency-Key header is required")
            }

            it("replays completed response for same key and same body") {
                val templateId = createTemplate()
                val userId = createUserWithIdempotencyHeader("same-body-user")
                val key = "idem-email-send-same-${System.currentTimeMillis()}"
                val request = SendNotificationEmailRequest(
                    campaignId = null,
                    templateId = templateId,
                    templateVersion = null,
                    userIds = listOf(userId)
                )

                val firstResponse = webTestClient.post()
                    .uri("/api/v1/emails/send/notifications")
                    .header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody<String>()
                    .returnResult()
                    .responseBody!!

                val secondResponse = webTestClient.post()
                    .uri("/api/v1/emails/send/notifications")
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
        }
    }

    private fun createTemplate(): Long {
        val templateRequest = PostTemplateRequest(
            templateName = "idem-template-${System.currentTimeMillis()}",
            subject = "Idempotency Test",
            body = "<h1>Hello ${'$'}{user_name}</h1>",
            variables = listOf("user_name")
        )

        val responseBody = webTestClient.post()
            .uri("/api/v1/emails/templates")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(templateRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .returnResult()
            .responseBody!!

        val node = objectMapper.readTree(responseBody)
        return node["data"]["id"].asLong()
    }

    private fun createUserWithIdempotencyHeader(suffix: String): Long {
        val timestamp = System.currentTimeMillis()
        val request = EnrollUserRequest(
            id = null,
            externalId = "idem-email-user-$suffix-$timestamp",
            userAttributes = """
            {
                "email": "idem-email-$suffix-$timestamp@example.com",
                "name": "Idempotency Email User"
            }
            """.trimIndent()
        )

        val responseBody = webTestClient.post()
            .uri("/api/v1/users")
            .header("Idempotency-Key", "idem-user-$suffix-$timestamp")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .returnResult()
            .responseBody!!

        val node = objectMapper.readTree(responseBody)
        return node["data"]["id"].asLong()
    }
}
