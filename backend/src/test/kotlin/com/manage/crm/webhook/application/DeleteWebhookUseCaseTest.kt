package com.manage.crm.webhook.application

import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.webhook.application.dto.DeleteWebhookUseCaseIn
import com.manage.crm.webhook.domain.repository.WebhookRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

class DeleteWebhookUseCaseTest : BehaviorSpec({
    lateinit var webhookRepository: WebhookRepository
    lateinit var deleteWebhookUseCase: DeleteWebhookUseCase

    beforeTest {
        webhookRepository = mockk()
        deleteWebhookUseCase = DeleteWebhookUseCase(webhookRepository)
    }

    afterTest { (_, _) ->
        clearMocks(webhookRepository)
    }

    given("delete webhook") {
        `when`("webhook is missing") {
            then("throw NotFoundByIdException and skip delete") {
                val webhookId = 123L
                coEvery { webhookRepository.findById(webhookId) } returns null

                shouldThrow<NotFoundByIdException> {
                    runBlocking { deleteWebhookUseCase.execute(DeleteWebhookUseCaseIn(webhookId)) }
                }

                coVerify(exactly = 1) { webhookRepository.findById(webhookId) }
                coVerify(exactly = 0) { webhookRepository.delete(any()) }
            }
        }
    }
})
