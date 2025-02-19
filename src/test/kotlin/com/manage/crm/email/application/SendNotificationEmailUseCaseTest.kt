package com.manage.crm.email.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.SendEmailDto
import com.manage.crm.email.application.dto.SendNotificationEmailUseCaseIn
import com.manage.crm.email.application.service.NonVariablesMailService
import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.EmailTemplateHistory
import com.manage.crm.email.domain.repository.EmailTemplateHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.Json
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs

class SendNotificationEmailUseCaseTest : BehaviorSpec({
    lateinit var emailTemplateRepository: EmailTemplateRepository
    lateinit var emailTemplateHistoryRepository: EmailTemplateHistoryRepository
    lateinit var nonVariablesEmailService: NonVariablesMailService
    lateinit var userRepository: UserRepository
    lateinit var useCase: SendNotificationEmailUseCase

    beforeContainer {
        emailTemplateRepository = mockk()
        emailTemplateHistoryRepository = mockk()
        nonVariablesEmailService = mockk()
        userRepository = mockk()
        useCase = SendNotificationEmailUseCase(
            emailTemplateRepository,
            emailTemplateHistoryRepository,
            nonVariablesEmailService,
            userRepository,
            ObjectMapper()
        )
    }

    fun userSubs(size: Int): List<User> =
        (1..size).map {
            User(
                id = it.toLong(),
                externalId = it.toString(),
                userAttributes = Json(
                    """
                    {
                        "email": "example$it@example.com"
                    }
                    """.trimIndent()
                )
            )
        }

    given("SendNotificationEmailUseCase") {
        `when`("send notification with latest template with users") {
            val useCaseIn = SendNotificationEmailUseCaseIn(
                templateId = 1,
                templateVersion = null,
                userIds = listOf(1L, 2L)
            )

            coEvery { emailTemplateRepository.findById(useCaseIn.templateId) } answers {
                EmailTemplate(
                    id = 1,
                    templateName = "templateName",
                    subject = "subject",
                    body = "body",
                    variables = Variables(),
                    version = 1.0f
                )
            }

            coEvery { userRepository.findAllByIdIn(useCaseIn.userIds) } answers { userSubs(useCaseIn.userIds.size) }

            coEvery { nonVariablesEmailService.send(any(SendEmailDto::class)) } just runs

            val result = useCase.execute(useCaseIn)
            then("should return SendNotificationEmailUseCaseOut") {
                result.isSuccess shouldBe true
            }

            then("find template by id") {
                coVerify(exactly = 1) { emailTemplateRepository.findById(useCaseIn.templateId) }
            }

            then("find users") {
                coVerify(exactly = 1) { userRepository.findAllByIdIn(useCaseIn.userIds) }
            }

            then("send notification email") {
                coVerify(exactly = 2) { nonVariablesEmailService.send(any(SendEmailDto::class)) }
            }
        }

        `when`("send notification with specific template version with users") {
            val useCaseIn = SendNotificationEmailUseCaseIn(
                templateId = 1,
                templateVersion = 1.1f,
                userIds = listOf(1L, 2L)
            )

            coEvery { emailTemplateHistoryRepository.findByTemplateIdAndVersion(useCaseIn.templateId, useCaseIn.templateVersion!!) } answers {
                EmailTemplateHistory(
                    id = 1,
                    templateId = 1,
                    subject = "subject",
                    body = "body",
                    variables = Variables(),
                    version = 1.1f
                )
            }

            coEvery { userRepository.findAllByIdIn(useCaseIn.userIds) } answers { userSubs(useCaseIn.userIds.size) }

            coEvery { nonVariablesEmailService.send(any(SendEmailDto::class)) } just runs

            then("should return SendNotificationEmailUseCaseOut") {
                val result = useCase.execute(useCaseIn)
                result.isSuccess shouldBe true
            }

            then("find template by id and version") {
                coVerify(exactly = 1) { emailTemplateHistoryRepository.findByTemplateIdAndVersion(useCaseIn.templateId, useCaseIn.templateVersion!!) }
            }

            then("find users") {
                coVerify(exactly = 1) { userRepository.findAllByIdIn(useCaseIn.userIds) }
            }

            then("send notification email") {
                coVerify(exactly = 2) { nonVariablesEmailService.send(any(SendEmailDto::class)) }
            }
        }

        `when`("send notification to all users") {
            val useCaseIn = SendNotificationEmailUseCaseIn(
                templateId = 1,
                templateVersion = null,
                userIds = emptyList()
            )

            coEvery { emailTemplateRepository.findById(useCaseIn.templateId) } answers {
                EmailTemplate(
                    id = 1,
                    templateName = "templateName",
                    subject = "subject",
                    body = "body",
                    variables = Variables(),
                    version = 1.0f
                )
            }

            val key = "email"
            coEvery { userRepository.findAllExistByUserAttributesKey(key) } answers { userSubs(useCaseIn.userIds.size) }

            coEvery { nonVariablesEmailService.send(any(SendEmailDto::class)) } just runs

            then("should return SendNotificationEmailUseCaseOut") {
                val result = useCase.execute(useCaseIn)
                result.isSuccess shouldBe true
            }

            then("find template by id") {
                coVerify(exactly = 1) { emailTemplateRepository.findById(useCaseIn.templateId) }
            }

            then("find all users which have attribute key") {
                coVerify(exactly = 1) { userRepository.findAllExistByUserAttributesKey(key) }
            }

            then("send notification email") {
                coVerify(exactly = 0) { nonVariablesEmailService.send(any(SendEmailDto::class)) }
            }
        }
    }
})
