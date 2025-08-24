package com.manage.crm.email.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.SendEmailInDto
import com.manage.crm.email.application.dto.SendEmailOutDto
import com.manage.crm.email.application.dto.SendNotificationEmailUseCaseIn
import com.manage.crm.email.application.dto.VariablesContent
import com.manage.crm.email.application.service.EmailContentService
import com.manage.crm.email.application.service.MailService
import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.EmailTemplateHistory
import com.manage.crm.email.domain.model.NotificationEmailTemplateVariablesModel
import com.manage.crm.email.domain.repository.EmailTemplateHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.support.VariablesSupport
import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.CampaignEvents
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import com.manage.crm.support.exception.NotFoundByException
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.UserFixtures
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.JsonFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class SendNotificationEmailUseCaseTest : BehaviorSpec({
    lateinit var emailTemplateRepository: EmailTemplateRepository
    lateinit var emailTemplateHistoryRepository: EmailTemplateHistoryRepository
    lateinit var mailService: MailService
    lateinit var emailContentService: EmailContentService
    lateinit var campaignRepository: CampaignRepository
    lateinit var campaignEventsRepository: CampaignEventsRepository
    lateinit var eventRepository: EventRepository
    lateinit var userRepository: UserRepository
    lateinit var useCase: SendNotificationEmailUseCase

    beforeContainer {
        emailTemplateRepository = mockk()
        emailTemplateHistoryRepository = mockk()
        mailService = mockk()
        emailContentService = mockk()
        campaignRepository = mockk()
        campaignEventsRepository = mockk()
        eventRepository = mockk()
        userRepository = mockk()
        useCase = SendNotificationEmailUseCase(
            emailTemplateRepository,
            emailTemplateHistoryRepository,
            mailService,
            emailContentService,
            campaignRepository,
            campaignEventsRepository,
            eventRepository,
            userRepository,
            ObjectMapper()
        )
    }

    fun userSubs(size: Int): List<User> = (1..size).map {
        UserFixtures.giveMeOne().withUserAttributes(
            JsonFixtures.giveMeOne().withValue(
                """
                    {
                        "email": "example$it@example.com",
                        "name": "name$it",
                        "detail": "{\"age\" : $it}"
                    }
                """.trimIndent()
            ).build()
        ).build()
    }

    given("SendNotificationEmailUseCase") {
        `when`("send notification with latest template with users") {
            val useCaseIn = SendNotificationEmailUseCaseIn(
                campaignId = null,
                templateId = 1,
                templateVersion = null,
                userIds = listOf(1L, 2L)
            )

            coEvery { emailTemplateRepository.findById(useCaseIn.templateId) } answers {
                EmailTemplate.new(
                    id = 1,
                    templateName = "templateName",
                    subject = "subject",
                    body = "body",
                    variables = Variables(),
                    version = 1.0f,
                    createdAt = LocalDateTime.now()
                )
            }

            coEvery { userRepository.findAllByIdIn(useCaseIn.userIds) } answers { userSubs(useCaseIn.userIds.size) }

            coEvery { emailContentService.genUserEmailContent(any(), any(), any()) } answers {
                NonContent()
            }

            coEvery { mailService.send(any(SendEmailInDto::class)) } answers {
                SendEmailOutDto(
                    userId = 1L,
                    emailBody = "emailBody",
                    messageId = "messageId",
                    destination = "destination",
                    provider = EmailProviderType.JAVA
                )
            }

            val result = useCase.execute(useCaseIn)
            then("should return SendNotificationEmailUseCaseOut") {
                result.isSuccess shouldBe true
            }

            then("find template by id") {
                coVerify(exactly = 1) { emailTemplateRepository.findById(useCaseIn.templateId) }
            }

            then("not called template by id and version") {
                coVerify(exactly = 0) {
                    emailTemplateHistoryRepository.findByTemplateIdAndVersion(
                        useCaseIn.templateId,
                        any(Float::class)
                    )
                }
            }

            then("find users") {
                coVerify(exactly = 1) { userRepository.findAllByIdIn(useCaseIn.userIds) }
            }

            then("generate user email content") {
                coVerify(exactly = 2) { emailContentService.genUserEmailContent(any(), any(), any()) }
            }

            then("send notification email") {
                coVerify(exactly = 2) { mailService.send(any(SendEmailInDto::class)) }
            }
        }

        `when`("send notification with specific template version with users") {
            val useCaseIn = SendNotificationEmailUseCaseIn(
                campaignId = null,
                templateId = 1,
                templateVersion = 1.1f,
                userIds = listOf(1L, 2L)
            )

            coEvery {
                emailTemplateHistoryRepository.findByTemplateIdAndVersion(
                    useCaseIn.templateId,
                    useCaseIn.templateVersion!!
                )
            } answers {
                EmailTemplateHistory.new(
                    id = 1,
                    templateId = 1,
                    subject = "subject",
                    body = "body",
                    variables = Variables(),
                    version = 1.1f,
                    createdAt = LocalDateTime.now()
                )
            }

            coEvery { userRepository.findAllByIdIn(useCaseIn.userIds) } answers { userSubs(useCaseIn.userIds.size) }

            coEvery { emailContentService.genUserEmailContent(any(), any(), any()) } answers {
                NonContent()
            }

            coEvery { mailService.send(any(SendEmailInDto::class)) } answers {
                SendEmailOutDto(
                    userId = 1L,
                    emailBody = "emailBody",
                    messageId = "messageId",
                    destination = "destination",
                    provider = EmailProviderType.JAVA
                )
            }

            then("should return SendNotificationEmailUseCaseOut") {
                val result = useCase.execute(useCaseIn)
                result.isSuccess shouldBe true
            }

            then("not called find template by id") {
                coVerify(exactly = 0) { emailTemplateRepository.findById(useCaseIn.templateId) }
            }

            then("find template by id and version") {
                coVerify(exactly = 1) {
                    emailTemplateHistoryRepository.findByTemplateIdAndVersion(
                        useCaseIn.templateId,
                        useCaseIn.templateVersion!!
                    )
                }
            }

            then("find users") {
                coVerify(exactly = 1) { userRepository.findAllByIdIn(useCaseIn.userIds) }
            }

            then("generate user email content") {
                coVerify(exactly = 2) { emailContentService.genUserEmailContent(any(), any(), any()) }
            }

            then("send notification email") {
                coVerify(exactly = 2) { mailService.send(any(SendEmailInDto::class)) }
            }
        }

        `when`("send notification to all users") {
            val useCaseIn = SendNotificationEmailUseCaseIn(
                campaignId = null,
                templateId = 1,
                templateVersion = null,
                userIds = emptyList()
            )

            coEvery { emailTemplateRepository.findById(useCaseIn.templateId) } answers {
                EmailTemplate.new(
                    id = 1,
                    templateName = "templateName",
                    subject = "subject",
                    body = "body",
                    variables = Variables(),
                    version = 1.0f,
                    createdAt = LocalDateTime.now()
                )
            }

            val key = "email"
            coEvery { userRepository.findAllExistByUserAttributesKey(key) } answers {
                userSubs(
                    useCaseIn.userIds.size
                )
            }

            coEvery { mailService.send(any(SendEmailInDto::class)) } answers {
                SendEmailOutDto(
                    userId = 1L,
                    emailBody = "emailBody",
                    messageId = "messageId",
                    destination = "destination",
                    provider = EmailProviderType.JAVA
                )
            }

            then("should return SendNotificationEmailUseCaseOut") {
                val result = useCase.execute(useCaseIn)
                result.isSuccess shouldBe true
            }

            then("find template by id") {
                coVerify(exactly = 1) { emailTemplateRepository.findById(useCaseIn.templateId) }
            }

            then("not called template by id and version") {
                coVerify(exactly = 0) {
                    emailTemplateHistoryRepository.findByTemplateIdAndVersion(
                        useCaseIn.templateId,
                        any(Float::class)
                    )
                }
            }

            then("find all users which have attribute key") {
                coVerify(exactly = 1) { userRepository.findAllExistByUserAttributesKey(key) }
            }

            then("not send notification email") {
                coVerify(exactly = 0) { mailService.send(any(SendEmailInDto::class)) }
            }
        }

        `when`("send notification with variable template") {
            val useCaseIn = SendNotificationEmailUseCaseIn(
                campaignId = null,
                templateId = 1,
                templateVersion = null,
                userIds = listOf(1L, 2L)
            )

            coEvery { emailTemplateRepository.findById(useCaseIn.templateId) } answers {
                EmailTemplate.new(
                    id = 1,
                    templateName = "templateName",
                    subject = "subject",
                    body = """
                        <html>
                            <head></head>
                            <body>
                                <a th:text="\$\{attribute_email}"></a>
                                <a th:text="\$\{attribute_name}"></a>
                                <a th:text="\$\{custom_detail_age}"></a>
                            </body>
                        </html>
                    """.trimIndent(),
                    variables = Variables(
                        listOf("attribute_email", "attribute_name", "custom_detail_age")
                    ),
                    version = 1.0f,
                    createdAt = LocalDateTime.now()
                )
            }

            coEvery { userRepository.findAllByIdIn(useCaseIn.userIds) } answers { userSubs(useCaseIn.userIds.size) }

            val objectMapper = ObjectMapper()
            coEvery { emailContentService.genUserEmailContent(any(), any(), any()) } answers { it ->
                val user = it.invocation.args[0] as User
                val notificationVariables = it.invocation.args[1] as NotificationEmailTemplateVariablesModel
                val campaignId = it.invocation.args[2] as Long?
                val attributes = user.userAttributes
                val variables = notificationVariables.variables
                variables.getVariables(false)
                    .associate { key ->
                        VariablesSupport.doAssociate(objectMapper, key, attributes, variables)
                    }.let {
                        VariablesContent(it)
                    }
            }

            coEvery { mailService.send(any(SendEmailInDto::class)) } answers {
                SendEmailOutDto(
                    userId = 1L,
                    emailBody = "emailBody",
                    messageId = "messageId",
                    destination = "destination",
                    provider = EmailProviderType.JAVA
                )
            }

            val result = useCase.execute(useCaseIn)
            then("should return SendNotificationEmailUseCaseOut") {
                result.isSuccess shouldBe true
            }

            then("find template by id") {
                coVerify(exactly = 1) { emailTemplateRepository.findById(useCaseIn.templateId) }
            }

            then("not called template by id and version") {
                coVerify(exactly = 0) {
                    emailTemplateHistoryRepository.findByTemplateIdAndVersion(
                        useCaseIn.templateId,
                        any(Float::class)
                    )
                }
            }

            then("find users") {
                coVerify(exactly = 1) { userRepository.findAllByIdIn(useCaseIn.userIds) }
            }

            then("generate user email content") {
                coVerify(exactly = 2) { emailContentService.genUserEmailContent(any(), any(), any()) }
            }

            then("send notification email") {
                coVerify(exactly = 2) { mailService.send(any(SendEmailInDto::class)) }
            }
        }

        `when`("send notification but not found by email template id and template version") {
            val useCaseIn = SendNotificationEmailUseCaseIn(
                campaignId = null,
                templateId = 1,
                templateVersion = 1.1f,
                userIds = listOf(1L, 2L)
            )

            coEvery {
                emailTemplateHistoryRepository.findByTemplateIdAndVersion(
                    useCaseIn.templateId,
                    useCaseIn.templateVersion!!
                )
            } answers {
                throw NotFoundByException(
                    "EmailTemplate",
                    "templateId",
                    useCaseIn.templateId,
                    "version",
                    useCaseIn.templateVersion!!
                )
            }

            then("should throw exception") {
                val exception = shouldThrow<NotFoundByException> {
                    useCase.execute(useCaseIn)
                }

                exception.message shouldBe "EmailTemplate not found by templateId and version: ${useCaseIn.templateId}, ${useCaseIn.templateVersion}"
            }

            then("not called find template by id") {
                coVerify(exactly = 0) { emailTemplateRepository.findById(useCaseIn.templateId) }
            }

            then("find template by id and version") {
                coVerify(exactly = 1) {
                    emailTemplateHistoryRepository.findByTemplateIdAndVersion(
                        useCaseIn.templateId,
                        useCaseIn.templateVersion!!
                    )
                }
            }

            then("not called find users") {
                coVerify(exactly = 0) { userRepository.findAllByIdIn(useCaseIn.userIds) }
            }

            then("not called send notification email") {
                coVerify(exactly = 0) { mailService.send(any(SendEmailInDto::class)) }
            }
        }

        `when`("send notification but not found by email template id") {
            val useCaseIn = SendNotificationEmailUseCaseIn(
                campaignId = null,
                templateId = 1,
                templateVersion = null,
                userIds = listOf(1L, 2L)
            )

            coEvery { emailTemplateRepository.findById(useCaseIn.templateId) } answers {
                throw NotFoundByIdException("EmailTemplate", useCaseIn.templateId)
            }

            then("should throw exception") {
                val exception = shouldThrow<NotFoundByIdException> {
                    useCase.execute(useCaseIn)
                }
                exception.message shouldBe "EmailTemplate not found by id: ${useCaseIn.templateId}"
            }

            then("find template by id") {
                coVerify(exactly = 1) { emailTemplateRepository.findById(useCaseIn.templateId) }
            }

            then("not called template by id and version") {
                coVerify(exactly = 0) {
                    emailTemplateHistoryRepository.findByTemplateIdAndVersion(
                        useCaseIn.templateId,
                        any(Float::class)
                    )
                }
            }

            then("not called find users") {
                coVerify(exactly = 0) { userRepository.findAllByIdIn(useCaseIn.userIds) }
            }

            then("not called send notification email") {
                coVerify(exactly = 0) { mailService.send(any(SendEmailInDto::class)) }
            }

            then("not called find users") {
                coVerify(exactly = 0) { userRepository.findAllByIdIn(useCaseIn.userIds) }
            }

            then("not called  send notification email") {
                coVerify(exactly = 0) { mailService.send(any(SendEmailInDto::class)) }
            }
        }

        `when`("send notification with campaign") {
            val campaignId = 1L
            val useCaseIn = SendNotificationEmailUseCaseIn(
                campaignId = campaignId,
                templateId = 1,
                templateVersion = null,
                userIds = listOf(1L, 2L)
            )

            val campaign = Campaign.new(
                campaignId,
                "Test Campaign",
                Properties(listOf(Property("campaignProp", "campaignValue"))),
                LocalDateTime.now()
            )

            val campaignEvents = listOf(
                CampaignEvents.new(
                    campaignId = campaignId,
                    eventId = 10L
                ),
                CampaignEvents.new(
                    campaignId = campaignId,
                    eventId = 11L
                )
            )

            val events = listOf(
                Event.new(
                    10L,
                    "Event1",
                    1L,
                    Properties(listOf(Property("eventProp", "eventValue1"))),
                    LocalDateTime.now()
                ),
                Event.new(
                    11L,
                    "Event2",
                    2L,
                    Properties(listOf(Property("eventProp", "eventValue2"))),
                    LocalDateTime.now()
                )
            )

            coEvery { campaignRepository.findById(campaignId) } returns campaign
            coEvery { campaignEventsRepository.findAllByCampaignId(campaignId) } returns campaignEvents
            coEvery { eventRepository.findAllByIdIn(listOf(10L, 11L)) } returns events

            coEvery { emailTemplateRepository.findById(useCaseIn.templateId) } answers {
                EmailTemplate.new(
                    id = 1,
                    templateName = "templateName",
                    subject = "subject",
                    body = "body",
                    variables = Variables(),
                    version = 1.0f,
                    createdAt = LocalDateTime.now()
                )
            }

            val usersInCampaign = userSubs(2).map { user ->
                when (user.id) {
                    1L -> user
                    2L -> user
                    else -> user
                }
            }
            coEvery { userRepository.findAllByIdIn(listOf(1L, 2L)) } returns usersInCampaign

            coEvery { emailContentService.genUserEmailContent(any(), any(), any()) } answers {
                NonContent()
            }

            coEvery { mailService.send(any(SendEmailInDto::class)) } answers {
                SendEmailOutDto(
                    userId = 1L,
                    emailBody = "emailBody",
                    messageId = "messageId",
                    destination = "destination",
                    provider = EmailProviderType.JAVA
                )
            }

            val result = useCase.execute(useCaseIn)

            then("should return SendNotificationEmailUseCaseOut") {
                result.isSuccess shouldBe true
            }

            then("find campaign by id") {
                coVerify(exactly = 1) { campaignRepository.findById(campaignId) }
            }

            then("find campaign events") {
                coVerify(exactly = 1) { campaignEventsRepository.findAllByCampaignId(campaignId) }
            }

            then("find events by id list") {
                coVerify(exactly = 1) { eventRepository.findAllByIdIn(listOf(10L, 11L)) }
            }

            then("find template by id") {
                coVerify(exactly = 1) { emailTemplateRepository.findById(useCaseIn.templateId) }
            }

            then("find users") {
                coVerify(exactly = 1) { userRepository.findAllByIdIn(listOf(1L, 2L)) }
            }

            then("generate user email content with campaign") {
                coVerify(exactly = 2) { emailContentService.genUserEmailContent(any(), any(), campaignId) }
            }

            then("send notification email") {
                coVerify(exactly = 2) { mailService.send(any(SendEmailInDto::class)) }
            }
        }
    }
})
