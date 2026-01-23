package com.manage.crm.webhook.application

import com.manage.crm.support.exception.AlreadyExistsException
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.webhook.application.dto.PostWebhookUseCaseIn
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEventType
import com.manage.crm.webhook.domain.WebhookEvents
import com.manage.crm.webhook.domain.repository.WebhookRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PostWebhookUseCaseTest : BehaviorSpec({
    lateinit var webhookRepository: WebhookRepository
    lateinit var postWebhookUseCase: PostWebhookUseCase

    beforeTest {
        webhookRepository = mockk()
        postWebhookUseCase = PostWebhookUseCase(webhookRepository)
    }

    afterTest { (_, _) ->
        clearMocks(webhookRepository)
    }

    given("create webhook") {
        `when`("request is valid and name is unique") {
            then("persist webhook and return response") {
                val request = PostWebhookUseCaseIn(
                    name = "user-webhook",
                    url = "https://example.com/webhook",
                    events = listOf(WebhookEventType.USER_CREATED.value),
                    active = null
                )
                val saved = Webhook.new(
                    id = 1L,
                    name = request.name,
                    url = request.url,
                    events = WebhookEvents.fromValues(request.events),
                    active = true,
                    createdAt = LocalDateTime.of(2024, 1, 1, 10, 30, 0)
                )

                coEvery { webhookRepository.findByName(request.name) } returns null
                coEvery { webhookRepository.save(any()) } returns saved

                val response = postWebhookUseCase.execute(request)

                coVerify(exactly = 1) { webhookRepository.findByName(request.name) }
                val savedSlot = slot<Webhook>()
                coVerify(exactly = 1) { webhookRepository.save(capture(savedSlot)) }
                savedSlot.captured.name shouldBe request.name
                savedSlot.captured.url shouldBe request.url
                savedSlot.captured.events.toValues() shouldContainExactly request.events
                savedSlot.captured.active shouldBe true

                response.id shouldBe saved.id!!
                response.name shouldBe request.name
                response.url shouldBe request.url
                response.events shouldContainExactly request.events
                response.active shouldBe true
                response.createdAt shouldBe saved.createdAt!!.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }
        }

        `when`("name already exists before save") {
            then("throw AlreadyExistsException") {
                val request = PostWebhookUseCaseIn(
                    name = "duplicated",
                    url = "https://example.com/webhook",
                    events = listOf(WebhookEventType.EMAIL_SENT.value),
                    active = true
                )

                coEvery { webhookRepository.findByName(request.name) } returns Webhook.new(
                    id = 5L,
                    name = request.name,
                    url = request.url,
                    events = WebhookEvents.fromValues(request.events),
                    active = true,
                    createdAt = LocalDateTime.now()
                )

                shouldThrow<AlreadyExistsException> {
                    runBlocking { postWebhookUseCase.execute(request) }
                }
            }
        }

        `when`("database reports duplicate name during save") {
            then("wrap duplicate violation as AlreadyExistsException") {
                val request = PostWebhookUseCaseIn(
                    name = "unique-check",
                    url = "https://example.com/webhook",
                    events = listOf(WebhookEventType.USER_CREATED.value),
                    active = true
                )

                coEvery { webhookRepository.findByName(request.name) } returns null
                coEvery { webhookRepository.save(any()) } throws DataIntegrityViolationException("duplicate")

                shouldThrow<AlreadyExistsException> {
                    runBlocking { postWebhookUseCase.execute(request) }
                }
            }
        }
    }

    given("update webhook") {
        `when`("webhook exists and partial fields are provided") {
            then("update only provided fields and return response") {
                val webhookId = 10L
                val existing = Webhook.new(
                    id = webhookId,
                    name = "old-name",
                    url = "https://old.example.com",
                    events = WebhookEvents(listOf(WebhookEventType.USER_CREATED)),
                    active = true,
                    createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
                )
                val request = PostWebhookUseCaseIn(
                    id = webhookId,
                    name = "new-name",
                    url = "https://old.example.com",
                    events = listOf("email_sent"),
                    active = false
                )

                coEvery { webhookRepository.findById(webhookId) } returns existing
                coEvery { webhookRepository.save(any()) } returns existing

                val response = postWebhookUseCase.execute(request)

                coVerify(exactly = 1) { webhookRepository.findById(webhookId) }
                val updatedSlot = slot<Webhook>()
                coVerify(exactly = 1) { webhookRepository.save(capture(updatedSlot)) }
                updatedSlot.captured.id shouldBe webhookId
                updatedSlot.captured.name shouldBe request.name
                updatedSlot.captured.url shouldBe existing.url
                updatedSlot.captured.events.toValues() shouldContainExactly listOf(WebhookEventType.EMAIL_SENT.value)
                updatedSlot.captured.active shouldBe false

                response.id shouldBe webhookId
                response.name shouldBe request.name
                response.url shouldBe existing.url
                response.events shouldContainExactly listOf(WebhookEventType.EMAIL_SENT.value)
                response.active shouldBe false
                response.createdAt shouldBe existing.createdAt!!.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }
        }

        `when`("webhook does not exist") {
            then("throw NotFoundByIdException") {
                val webhookId = 99L
                val request = PostWebhookUseCaseIn(
                    id = webhookId,
                    name = "any",
                    url = "https://example.com",
                    events = listOf(WebhookEventType.USER_CREATED.value),
                    active = true
                )

                coEvery { webhookRepository.findById(webhookId) } returns null

                shouldThrow<NotFoundByIdException> {
                    runBlocking { postWebhookUseCase.execute(request) }
                }
            }
        }
    }
})
