package com.manage.crm.email.application

import com.manage.crm.email.application.dto.PostTemplateUseCaseIn
import com.manage.crm.email.application.dto.PostTemplateUseCaseOut
import com.manage.crm.email.application.service.HtmlService
import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.EmailTemplateHistory
import com.manage.crm.email.domain.repository.EmailTemplateHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.email.event.template.PostEmailTemplateEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.springframework.context.ApplicationEventPublisher

class PostTemplateUseCaseTest : BehaviorSpec({
    lateinit var emailTemplateRepository: EmailTemplateRepository
    lateinit var emailTemplateHistoryRepository: EmailTemplateHistoryRepository
    lateinit var htmlService: HtmlService
    lateinit var applicationEventPublisher: ApplicationEventPublisher
    lateinit var useCase: PostTemplateUseCase

    beforeContainer {
        emailTemplateRepository = mockk()
        emailTemplateHistoryRepository = mockk()
        htmlService = mockk()
        applicationEventPublisher = mockk()
        useCase = PostTemplateUseCase(
            emailTemplateRepository,
            emailTemplateHistoryRepository,
            htmlService,
            applicationEventPublisher
        )
    }

    given("PostTemplateUseCase") {
        `when`("create new template") {
            val useCaseIn = PostTemplateUseCaseIn(
                id = null,
                templateName = "templateName",
                subject = "subject",
                version = null,
                body = "body",
                variables = emptyList()
            )

            coEvery { htmlService.prettyPrintHtml(useCaseIn.body) } answers {
                useCaseIn.body
            }

            coEvery { htmlService.extractVariables(useCaseIn.body) } answers {
                useCaseIn.variables
            }

            coEvery { emailTemplateRepository.findByTemplateName(useCaseIn.templateName) } answers {
                null
            }

            val newEmailTemplate = EmailTemplate.new(
                templateName = useCaseIn.templateName,
                subject = useCaseIn.subject!!,
                body = useCaseIn.body,
                variables = Variables(useCaseIn.variables)
            ).apply {
                // set id after save
                id = 1
            }
            coEvery { emailTemplateRepository.save(any(EmailTemplate::class)) } answers { newEmailTemplate }

            val emailTemplateHistory = EmailTemplateHistory(
                templateId = newEmailTemplate.id!!,
                subject = newEmailTemplate.subject,
                body = newEmailTemplate.body,
                variables = newEmailTemplate.variables,
                version = newEmailTemplate.version
            )
            coEvery { emailTemplateHistoryRepository.save(any(EmailTemplateHistory::class)) } answers { emailTemplateHistory }

            coEvery { applicationEventPublisher.publishEvent(any(PostEmailTemplateEvent::class)) } just runs

            val result = useCase.execute(useCaseIn)
            then("should return BrowseEmailNotificationSchedulesUseCaseOut") {
                result shouldBe PostTemplateUseCaseOut(
                    id = newEmailTemplate.id!!,
                    templateName = newEmailTemplate.templateName!!,
                    version = newEmailTemplate.version
                )
            }

            then("pretty useCaseIn.body") {
                coVerify(exactly = 1) { htmlService.prettyPrintHtml(useCaseIn.body) }
            }

            then("extract variables") {
                coVerify(exactly = 1) { htmlService.extractVariables(useCaseIn.body) }
            }

            then("check if template exists") {
                coVerify(exactly = 1) { emailTemplateRepository.findByTemplateName(useCaseIn.templateName) }
            }

            then("save new template") {
                coVerify(exactly = 1) { emailTemplateRepository.save(any(EmailTemplate::class)) }
            }

            then("save new template history") {
                coVerify(exactly = 1) { emailTemplateHistoryRepository.save(any(EmailTemplateHistory::class)) }
            }

            then("publish save template event") {
                coVerify(exactly = 1) { applicationEventPublisher.publishEvent(any(PostEmailTemplateEvent::class)) }
            }
        }

        `when`("modify existing template") {
            val useCaseIn = PostTemplateUseCaseIn(
                id = 1,
                templateName = "templateName",
                subject = "modified subject",
                version = 1.1f,
                body = "modified body",
                variables = emptyList()
            )

            coEvery { htmlService.prettyPrintHtml(useCaseIn.body) } answers {
                useCaseIn.body
            }

            coEvery { htmlService.extractVariables(useCaseIn.body) } answers {
                useCaseIn.variables
            }
            val emailTemplate = EmailTemplate.new(
                templateName = useCaseIn.templateName,
                subject = "subject",
                body = "body",
                variables = Variables(emptyList())
            ).apply {
                id = useCaseIn.id
                version = 1.0f
            }
            coEvery { emailTemplateRepository.findById(useCaseIn.id!!) } answers { emailTemplate }

            val modifiedEmailTemplate = EmailTemplate.new(
                templateName = useCaseIn.templateName,
                subject = useCaseIn.subject!!,
                body = useCaseIn.body,
                variables = Variables(useCaseIn.variables)
            ).apply {
                id = useCaseIn.id
                version = useCaseIn.version!!
            }
            coEvery { emailTemplateRepository.save(any(EmailTemplate::class)) } answers { modifiedEmailTemplate }

            coEvery { applicationEventPublisher.publishEvent(any(PostEmailTemplateEvent::class)) } just runs

            val result = useCase.execute(useCaseIn)
            then("should return BrowseEmailNotificationSchedulesUseCaseOut") {
                result shouldBe PostTemplateUseCaseOut(
                    id = emailTemplate.id!!,
                    templateName = emailTemplate.templateName!!,
                    version = emailTemplate.version
                )
            }

            then("pretty useCaseIn.body") {
                coVerify(exactly = 1) { htmlService.prettyPrintHtml(useCaseIn.body) }
            }

            then("extract variables") {
                coVerify(exactly = 1) { htmlService.extractVariables(useCaseIn.body) }
            }

            then("find existing template") {
                coVerify(exactly = 1) { emailTemplateRepository.findById(useCaseIn.id!!) }
            }

            then("update template") {
                coVerify(exactly = 1) { emailTemplateRepository.save(any(EmailTemplate::class)) }
            }

            then("publish update template event") {
                coVerify(exactly = 1) { applicationEventPublisher.publishEvent(any(PostEmailTemplateEvent::class)) }
            }
        }
    }
})
