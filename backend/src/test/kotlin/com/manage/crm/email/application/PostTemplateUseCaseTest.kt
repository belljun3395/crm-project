package com.manage.crm.email.application

import com.manage.crm.email.application.dto.PostTemplateUseCaseIn
import com.manage.crm.email.application.dto.PostTemplateUseCaseOut
import com.manage.crm.email.application.service.EmailTemplateRepositoryEventProcessor
import com.manage.crm.email.application.service.HtmlService
import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.vo.EmailTemplateVersion
import com.manage.crm.email.domain.vo.Variables
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class PostTemplateUseCaseTest : BehaviorSpec({
    lateinit var emailTemplateRepository: EmailTemplateRepository
    lateinit var emailTemplateSaveRepository: EmailTemplateRepositoryEventProcessor
    lateinit var htmlService: HtmlService
    lateinit var useCase: PostTemplateUseCase

    beforeContainer {
        emailTemplateRepository = mockk()
        emailTemplateSaveRepository = mockk()
        htmlService = mockk()
        useCase = PostTemplateUseCase(
            emailTemplateRepository,
            emailTemplateSaveRepository,
            htmlService
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
            coEvery { emailTemplateSaveRepository.save(any(EmailTemplate::class)) } answers { newEmailTemplate }

            val result = useCase.execute(useCaseIn)
            then("should return BrowseEmailNotificationSchedulesUseCaseOut") {
                result shouldBe PostTemplateUseCaseOut(
                    id = newEmailTemplate.id!!,
                    templateName = newEmailTemplate.templateName!!,
                    version = newEmailTemplate.version!!.value
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

            then("save new template and publish event") {
                coVerify(exactly = 1) { emailTemplateSaveRepository.save(any(EmailTemplate::class)) }
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
                version = EmailTemplateVersion(1.0f)
            }
            coEvery { emailTemplateRepository.findById(useCaseIn.id!!) } answers { emailTemplate }

            val modifiedEmailTemplate = EmailTemplate.new(
                templateName = useCaseIn.templateName,
                subject = useCaseIn.subject!!,
                body = useCaseIn.body,
                variables = Variables(useCaseIn.variables)
            ).apply {
                id = useCaseIn.id
                version = EmailTemplateVersion(useCaseIn.version!!)
            }
            coEvery { emailTemplateSaveRepository.save(any(EmailTemplate::class)) } answers { modifiedEmailTemplate }

            val result = useCase.execute(useCaseIn)
            then("should return BrowseEmailNotificationSchedulesUseCaseOut") {
                result shouldBe PostTemplateUseCaseOut(
                    id = emailTemplate.id!!,
                    templateName = emailTemplate.templateName!!,
                    version = emailTemplate.version!!.value
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

            then("update template and publish event") {
                coVerify(exactly = 1) { emailTemplateSaveRepository.save(any(EmailTemplate::class)) }
            }
        }

        `when`("create new template with variable") {
            val useCaseIn = PostTemplateUseCaseIn(
                id = null,
                templateName = "templateName",
                subject = "subject",
                version = null,
                body = "body with variable \${attribute_email}",
                variables = listOf("attribute_email")
            )

            coEvery { htmlService.prettyPrintHtml(useCaseIn.body) } answers {
                useCaseIn.body
            }

            coEvery { htmlService.extractVariables(useCaseIn.body) } answers {
                useCaseIn.variables.map { it.substringBefore(":") }
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
            coEvery { emailTemplateSaveRepository.save(any(EmailTemplate::class)) } answers { newEmailTemplate }

            val result = useCase.execute(useCaseIn)
            then("should return BrowseEmailNotificationSchedulesUseCaseOut") {
                result shouldBe PostTemplateUseCaseOut(
                    id = newEmailTemplate.id!!,
                    templateName = newEmailTemplate.templateName!!,
                    version = newEmailTemplate.version!!.value
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

            then("save new template and publish event") {
                coVerify(exactly = 1) { emailTemplateSaveRepository.save(any(EmailTemplate::class)) }
            }
        }

        `when`("create new template with invalid variable") {
            val useCaseIn = PostTemplateUseCaseIn(
                id = null,
                templateName = "templateName",
                subject = "subject",
                version = null,
                body = "body with variable \${attribute_email}",
                variables = listOf("attribute_name")
            )

            coEvery { htmlService.prettyPrintHtml(useCaseIn.body) } answers {
                useCaseIn.body
            }

            val bodyVariables = listOf("attribute_email")
            coEvery { htmlService.extractVariables(useCaseIn.body) } answers {
                bodyVariables
            }

            val exception = shouldThrow<IllegalArgumentException> { useCase.execute(useCaseIn) }

            then("should throw IllegalArgumentException") {
                exception.message shouldBe "Variables do not match: \n$bodyVariables != ${useCaseIn.variables}"
            }

            then("pretty useCaseIn.body") {
                coVerify(exactly = 1) { htmlService.prettyPrintHtml(useCaseIn.body) }
            }

            then("extract variables") {
                coVerify(exactly = 1) { htmlService.extractVariables(useCaseIn.body) }
            }
        }

        `when`("create new template with invalid body to extract variables") {
            val useCaseIn = PostTemplateUseCaseIn(
                id = null,
                templateName = "templateName",
                subject = "subject",
                version = null,
                body = "body with variable \${{attribute_email}}",
                variables = listOf("attribute_email")
            )

            coEvery { htmlService.prettyPrintHtml(useCaseIn.body) } answers {
                useCaseIn.body
            }

            val invalidBodyVariables = listOf("{attribute_email}")
            coEvery { htmlService.extractVariables(useCaseIn.body) } answers {
                invalidBodyVariables
            }

            val exception = shouldThrow<IllegalArgumentException> { useCase.execute(useCaseIn) }

            then("should throw IllegalArgumentException") {
                exception.message shouldBe "Variables do not match: \n$invalidBodyVariables != ${useCaseIn.variables}"
            }

            then("pretty useCaseIn.body") {
                coVerify(exactly = 1) { htmlService.prettyPrintHtml(useCaseIn.body) }
            }

            then("extract variables") {
                coVerify(exactly = 1) { htmlService.extractVariables(useCaseIn.body) }
            }
        }

        `when`("create new template with variable and default") {
            val useCaseIn = PostTemplateUseCaseIn(
                id = null,
                templateName = "templateName",
                subject = "subject",
                version = null,
                body = "body with variable \${attribute_email}",
                variables = listOf("attribute_email:test@gmail.com")
            )

            coEvery { htmlService.prettyPrintHtml(useCaseIn.body) } answers {
                useCaseIn.body
            }

            coEvery { htmlService.extractVariables(useCaseIn.body) } answers {
                useCaseIn.variables.map { it.substringBefore(":") }
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
            coEvery { emailTemplateSaveRepository.save(any(EmailTemplate::class)) } answers { newEmailTemplate }

            val result = useCase.execute(useCaseIn)
            then("should return BrowseEmailNotificationSchedulesUseCaseOut") {
                result shouldBe PostTemplateUseCaseOut(
                    id = newEmailTemplate.id!!,
                    templateName = newEmailTemplate.templateName!!,
                    version = newEmailTemplate.version!!.value
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

            then("save new template and publish event") {
                coVerify(exactly = 1) { emailTemplateSaveRepository.save(any(EmailTemplate::class)) }
            }
        }
    }
})
