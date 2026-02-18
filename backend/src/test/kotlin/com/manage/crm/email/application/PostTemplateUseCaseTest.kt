package com.manage.crm.email.application

import com.manage.crm.email.application.dto.PostTemplateUseCaseIn
import com.manage.crm.email.application.dto.PostTemplateUseCaseOut
import com.manage.crm.email.application.service.EmailTemplateRepositoryEventProcessor
import com.manage.crm.email.application.service.HtmlService
import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.EmailTemplateFixtures
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.vo.EmailTemplateVersionFixtures
import com.manage.crm.email.exception.VariablesNotMatchException
import com.manage.crm.support.exception.DuplicateByException
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

            val newEmailTemplate = EmailTemplateFixtures.giveMeOne().withId(1L).build()
            coEvery { emailTemplateSaveRepository.save(any(EmailTemplate::class)) } answers { newEmailTemplate }

            val result = useCase.execute(useCaseIn)
            then("should return BrowseEmailNotificationSchedulesUseCaseOut") {
                result shouldBe PostTemplateUseCaseOut(
                    id = newEmailTemplate.id!!,
                    templateName = newEmailTemplate.templateName,
                    version = newEmailTemplate.version.value
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
            val emailTemplateFixtures = EmailTemplateFixtures.giveMeOne().withId(useCaseIn.id!!)
                .withVersion(EmailTemplateVersionFixtures.giveMeOne().withValue(1.0f).build())
            val emailTemplate = emailTemplateFixtures.build()
            coEvery { emailTemplateRepository.findById(useCaseIn.id!!) } answers { emailTemplate }

            val modifiedEmailTemplate = emailTemplateFixtures.withVersion(EmailTemplateVersionFixtures.giveMeOne().withValue(useCaseIn.version!!).build()).build()
            coEvery { emailTemplateSaveRepository.save(any(EmailTemplate::class)) } answers { modifiedEmailTemplate }

            val result = useCase.execute(useCaseIn)
            then("should return BrowseEmailNotificationSchedulesUseCaseOut") {
                result shouldBe PostTemplateUseCaseOut(
                    id = emailTemplate.id!!,
                    templateName = emailTemplate.templateName,
                    version = emailTemplate.version.value
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
                variables = listOf("user_email")
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

            val newEmailTemplate = EmailTemplateFixtures.giveMeOne().withId(1L).build()
            coEvery { emailTemplateSaveRepository.save(any(EmailTemplate::class)) } answers { newEmailTemplate }

            val result = useCase.execute(useCaseIn)
            then("should return BrowseEmailNotificationSchedulesUseCaseOut") {
                result shouldBe PostTemplateUseCaseOut(
                    id = newEmailTemplate.id!!,
                    templateName = newEmailTemplate.templateName,
                    version = newEmailTemplate.version.value
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

        `when`("create new template with not match variable") {
            val useCaseIn = PostTemplateUseCaseIn(
                id = null,
                templateName = "templateName",
                subject = "subject",
                version = null,
                body = "body with variable \${attribute_email}",
                variables = listOf("user_name")
            )

            coEvery { htmlService.prettyPrintHtml(useCaseIn.body) } answers {
                useCaseIn.body
            }

            val bodyVariables = listOf("user_email")
            coEvery { htmlService.extractVariables(useCaseIn.body) } answers {
                bodyVariables
            }

            val exception = shouldThrow<VariablesNotMatchException> { useCase.execute(useCaseIn) }

            then("should throw VariablesNotMatchException") {
                // displayValue() now uses the new source.key format (user.email, user.name)
                exception.message shouldBe "Variables do not match: \n[user.email] != [user.name]"
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
                variables = listOf("user_email")
            )

            coEvery { htmlService.prettyPrintHtml(useCaseIn.body) } answers {
                useCaseIn.body
            }

            val invalidBodyVariables = listOf("{user_email}")
            coEvery { htmlService.extractVariables(useCaseIn.body) } answers {
                invalidBodyVariables
            }

            val exception = shouldThrow<IllegalArgumentException> { useCase.execute(useCaseIn) }

            then("should throw IllegalArgumentException for unknown variable source") {
                exception.message?.contains("Unknown variable source") shouldBe true
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
                variables = listOf("user_email:test@gmail.com")
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

            val newEmailTemplate = EmailTemplateFixtures.giveMeOne().withId(1L).build()
            coEvery { emailTemplateSaveRepository.save(any(EmailTemplate::class)) } answers { newEmailTemplate }

            val result = useCase.execute(useCaseIn)
            then("should return BrowseEmailNotificationSchedulesUseCaseOut") {
                result shouldBe PostTemplateUseCaseOut(
                    id = newEmailTemplate.id!!,
                    templateName = newEmailTemplate.templateName,
                    version = newEmailTemplate.version.value
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

        `when`("create new template with campaign variable") {
            val useCaseIn = PostTemplateUseCaseIn(
                id = null,
                templateName = "templateName",
                subject = "subject",
                version = null,
                body = "body with campaign variable \${campaign_eventCount}",
                variables = listOf("campaign_eventCount")
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

            val newEmailTemplate = EmailTemplateFixtures.giveMeOne().withId(1L).build()
            coEvery { emailTemplateSaveRepository.save(any(EmailTemplate::class)) } answers { newEmailTemplate }

            val result = useCase.execute(useCaseIn)
            then("should return PostTemplateUseCaseOut") {
                result shouldBe PostTemplateUseCaseOut(
                    id = newEmailTemplate.id!!,
                    templateName = newEmailTemplate.templateName,
                    version = newEmailTemplate.version.value
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

        `when`("create new template with mixed user and campaign variables") {
            val useCaseIn = PostTemplateUseCaseIn(
                id = null,
                templateName = "templateName",
                subject = "subject",
                version = null,
                body = "<html><body><p>Hello <span th:text=\"\${user_name}\"></span></p><p>Email: <span th:text=\"\${user_email}\"></span></p><p>Event Count: <span th:text=\"\${campaign_eventCount}\"></span></p></body></html>",
                variables = listOf("user_name", "user_email", "campaign_eventCount")
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

            val newEmailTemplate = EmailTemplateFixtures.giveMeOne().withId(1L).build()
            coEvery { emailTemplateSaveRepository.save(any(EmailTemplate::class)) } answers { newEmailTemplate }

            val result = useCase.execute(useCaseIn)
            then("should return PostTemplateUseCaseOut") {
                result shouldBe PostTemplateUseCaseOut(
                    id = newEmailTemplate.id!!,
                    templateName = newEmailTemplate.templateName,
                    version = newEmailTemplate.version.value
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

        `when`("create new template with duplicate template name") {
            val useCaseIn = PostTemplateUseCaseIn(
                id = null,
                templateName = "templateName",
                subject = "subject",
                version = null,
                body = "body with variable \${attribute_email}",
                variables = listOf("user_email:test@gmail.com")
            )

            coEvery { htmlService.prettyPrintHtml(useCaseIn.body) } answers {
                useCaseIn.body
            }

            coEvery { htmlService.extractVariables(useCaseIn.body) } answers {
                useCaseIn.variables
            }

            coEvery { emailTemplateRepository.findByTemplateName(useCaseIn.templateName) } answers {
                throw DuplicateByException("EmailTemplate", "templateName", useCaseIn.templateName)
            }

            then("should return exception") {
                val exception = shouldThrow<DuplicateByException> { useCase.execute(useCaseIn) }
                exception.message shouldBe "Duplicate EmailTemplate by templateName: ${useCaseIn.templateName}"
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

            then("not called save new template and publish event") {
                coVerify(exactly = 0) { emailTemplateSaveRepository.save(any(EmailTemplate::class)) }
            }
        }
    }
})
