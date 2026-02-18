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
import com.manage.crm.email.domain.repository.EmailTemplateHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.vo.CampaignVariable
import com.manage.crm.email.domain.vo.EmailProviderType
import com.manage.crm.email.domain.vo.UserVariable
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.service.CampaignEventsService
import com.manage.crm.support.exception.NotFoundByException
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.UserFixtures
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.UserAttributesFixtures
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
    lateinit var campaignEventsService: CampaignEventsService
    lateinit var campaignRepository: CampaignRepository
    lateinit var userRepository: UserRepository
    lateinit var useCase: SendNotificationEmailUseCase

    beforeContainer {
        emailTemplateRepository = mockk()
        emailTemplateHistoryRepository = mockk()
        mailService = mockk()
        emailContentService = mockk()
        campaignEventsService = mockk()
        campaignRepository = mockk()
        userRepository = mockk()
        useCase = SendNotificationEmailUseCase(
            emailTemplateRepository,
            emailTemplateHistoryRepository,
            mailService,
            emailContentService,
            campaignEventsService,
            campaignRepository,
            userRepository,
            ObjectMapper()
        )
    }

    fun userSubs(size: Int): List<User> = (1..size).map {
        UserFixtures.giveMeOne().withUserAttributes(
            UserAttributesFixtures.giveMeOne().withValue(
                """
                    {
                        "email": "example$it@example.com",
                        "name": "User$it"
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

            coEvery { campaignEventsService.findAllEventsByCampaignIdAndUserId(any()) } returns emptyList()

            coEvery { campaignRepository.findById(any()) } returns null

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

            coEvery { campaignEventsService.findAllEventsByCampaignIdAndUserId(any()) } returns emptyList()

            coEvery { campaignRepository.findById(any()) } returns null

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
                emptyList()
            }

            coEvery { campaignEventsService.findAllEventsByCampaignIdAndUserId(any()) } returns emptyList()

            coEvery { campaignRepository.findById(any()) } returns null

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
                result.isSuccess shouldBe false
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
                                <a th:text="\$\{user_email}"></a>
                                <a th:text="\$\{user_name}"></a>
                            </body>
                        </html>
                    """.trimIndent(),
                    variables = Variables(
                        listOf(
                            UserVariable("email"),
                            UserVariable("name")
                        )
                    ),
                    version = 1.0f,
                    createdAt = LocalDateTime.now()
                )
            }

            coEvery { userRepository.findAllByIdIn(useCaseIn.userIds) } answers { userSubs(useCaseIn.userIds.size) }

            coEvery { campaignEventsService.findAllEventsByCampaignIdAndUserId(any()) } returns emptyList()

            coEvery { campaignRepository.findById(any()) } returns null

            coEvery { emailContentService.genUserEmailContent(any(), any(), any()) } answers {
                VariablesContent(mapOf("user_email" to "example1@example.com", "user_name" to "User1"))
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
        }

        `when`("send notification with campaign") {
            val useCaseIn = SendNotificationEmailUseCaseIn(
                campaignId = 1L,
                templateId = 1,
                templateVersion = null,
                userIds = listOf(1L, 2L)
            )

            coEvery { emailTemplateRepository.findById(useCaseIn.templateId) } answers {
                EmailTemplate.new(
                    id = 1,
                    templateName = "templateName",
                    subject = "subject",
                    body = "<html><head></head><body><a th:text=\"\${user_email}\"></a><a th:text=\"\${user_name}\"></a><a th:text=\"\${campaign_eventCount}\"></a></body></html>",
                    variables = Variables(
                        listOf(
                            UserVariable("email"),
                            UserVariable("name"),
                            CampaignVariable("eventCount")
                        )
                    ),
                    version = 1.0f,
                    createdAt = LocalDateTime.now()
                )
            }

            coEvery { userRepository.findAllByIdIn(useCaseIn.userIds) } answers { userSubs(useCaseIn.userIds.size) }

            val mockCampaign = Campaign.new(
                id = 1L,
                name = "Test Campaign",
                properties = CampaignProperties(listOf(CampaignProperty("eventCount", "10"))),
                createdAt = LocalDateTime.now()
            )
            coEvery { campaignRepository.findById(useCaseIn.campaignId!!) } returns mockCampaign

            val mockCampaignEvents = listOf(
                Event.new(
                    id = 1L,
                    name = "test_event",
                    userId = 1L,
                    properties = EventProperties(emptyList()),
                    createdAt = LocalDateTime.now()
                ),
                Event.new(
                    id = 2L,
                    name = "test_event",
                    userId = 2L,
                    properties = EventProperties(emptyList()),
                    createdAt = LocalDateTime.now()
                )
            )
            coEvery { campaignEventsService.findAllEventsByCampaignIdAndUserId(useCaseIn.campaignId!!) } returns mockCampaignEvents

            coEvery { emailContentService.genUserEmailContent(any(), any(), any()) } answers {
                VariablesContent(
                    mapOf(
                        "user_email" to "example1@example.com",
                        "user_name" to "User1",
                        "campaign_eventCount" to "10"
                    )
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

            then("find campaign by id") {
                coVerify(exactly = 1) { campaignRepository.findById(useCaseIn.campaignId!!) }
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

        `when`("send notification with campaign but campaign not found") {
            val useCaseIn = SendNotificationEmailUseCaseIn(
                campaignId = 999L,
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

            coEvery { campaignRepository.findById(useCaseIn.campaignId!!) } returns null

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

            then("should return success result") {
                val result = useCase.execute(useCaseIn)
                result.isSuccess shouldBe true
            }

            then("find template by id") {
                coVerify(exactly = 1) { emailTemplateRepository.findById(useCaseIn.templateId) }
            }

            then("find campaign by id") {
                coVerify(exactly = 1) { campaignRepository.findById(useCaseIn.campaignId!!) }
            }

            then("find users") {
                coVerify(exactly = 1) { userRepository.findAllByIdIn(useCaseIn.userIds) }
            }

            then("send notification email") {
                coVerify(exactly = 2) { mailService.send(any(SendEmailInDto::class)) }
            }
        }
    }
})
