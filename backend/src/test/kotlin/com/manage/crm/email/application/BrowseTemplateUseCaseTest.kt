package com.manage.crm.email.application

import com.manage.crm.email.application.dto.BrowseTemplateUseCaseIn
import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.EmailTemplateHistory
import com.manage.crm.email.domain.repository.EmailTemplateHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.vo.Variables
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import java.time.LocalDateTime

class BrowseTemplateUseCaseTest : BehaviorSpec({
    lateinit var emailTemplateRepository: EmailTemplateRepository
    lateinit var emailTemplateHistoryRepository: EmailTemplateHistoryRepository
    lateinit var useCase: BrowseTemplateUseCase

    beforeContainer {
        emailTemplateRepository = mockk()
        emailTemplateHistoryRepository = mockk()
        useCase = BrowseTemplateUseCase(emailTemplateRepository, emailTemplateHistoryRepository)
    }

    fun emailTemplateStubs(size: Int) = (1..size).map { it ->
        EmailTemplate.new(
            id = it.toLong(),
            templateName = "templateName$it",
            subject = "subject$it",
            body = "body$it",
            variables = Variables(),
            version = 1.0f,
            createdAt = LocalDateTime.now()
        )
    }

    given("BrowseTemplateUseCase") {
        `when`("browse templates with histories") {
            val useCaseIn = BrowseTemplateUseCaseIn(withHistory = true)
            fun emailTemplateHistoryStub() = listOf(
                EmailTemplateHistory(
                    id = 1,
                    templateId = 1,
                    subject = "subject1",
                    body = "body1",
                    variables = Variables(),
                    version = 1.0f
                ),
                // ----------------- template Id 2 is modified once -----------------
                EmailTemplateHistory.new(
                    id = 2,
                    templateId = 2,
                    subject = "subject2",
                    body = "body2",
                    variables = Variables(),
                    version = 1.0f,
                    createdAt = LocalDateTime.now()
                ),
                EmailTemplateHistory.new(
                    id = 3,
                    templateId = 2,
                    subject = "subject2.1",
                    body = "body2.1",
                    variables = Variables(),
                    version = 1.1f,
                    createdAt = LocalDateTime.now()
                ),
                EmailTemplateHistory.new(
                    id = 4,
                    templateId = 3,
                    subject = "subject3",
                    body = "body3",
                    variables = Variables(),
                    version = 1.0f,
                    createdAt = LocalDateTime.now()
                )
            )

            val emailTemplateStubSize = 3
            val emailTemplateStubs = emailTemplateStubs(emailTemplateStubSize)
            coEvery { emailTemplateRepository.findAll() } answers { emailTemplateStubs.asFlow() }

            coEvery {
                emailTemplateHistoryRepository.findAllByTemplateIdInOrderByVersionDesc(
                    emailTemplateStubs.map { it.id!! }
                )
            } answers { emailTemplateHistoryStub() }

            val result = useCase.execute(useCaseIn)
            then("should return BrowseTemplateUseCaseOut with histories") {
                result.templates.size shouldBe emailTemplateStubSize

                val templates = result.templates
                val template1 = templates[0]
                template1.template.id shouldBe 1
                template1.histories.size shouldBe 1

                val template2 = templates[1]
                template2.template.id shouldBe 2
                template2.histories.size shouldBe 2
                template2.histories[0].templateId shouldBe 2
                template2.histories[1].templateId shouldBe 2

                val template3 = templates[2]
                template3.template.id shouldBe 3
                template3.histories.size shouldBe 1
            }

            then("find all templates") {
                coVerify(exactly = 1) { emailTemplateRepository.findAll() }
            }

            then("find all template histories by template ids") {
                coVerify(exactly = 1) {
                    emailTemplateHistoryRepository.findAllByTemplateIdInOrderByVersionDesc(
                        emailTemplateStubs.map { it.id!! }
                    )
                }
            }
        }

        `when`("browse templates without histories") {
            val useCaseIn = BrowseTemplateUseCaseIn(withHistory = false)

            val emailTemplateStubSize = 3
            val emailTemplateStubs = emailTemplateStubs(emailTemplateStubSize)
            coEvery { emailTemplateRepository.findAll() } answers { emailTemplateStubs.asFlow() }

            val result = useCase.execute(useCaseIn)
            then("should return BrowseTemplateUseCaseOut without histories") {
                result.templates.size shouldBe emailTemplateStubSize

                val templates = result.templates
                for (i in 0 until emailTemplateStubSize) {
                    val template = templates[i]
                    template.histories.size shouldBe 0
                }
            }

            then("find all templates") {
                coVerify(exactly = 1) { emailTemplateRepository.findAll() }
            }
        }
    }
})
