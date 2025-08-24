package com.manage.crm.email.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.VariablesContent
import com.manage.crm.email.domain.model.NotificationEmailTemplateVariablesModel
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.event.domain.CampaignEvents
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import com.manage.crm.user.domain.UserFixtures
import com.manage.crm.user.domain.vo.JsonFixtures
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDateTime

class EmailContentServiceTest : BehaviorSpec({
    lateinit var campaignEventsRepository: CampaignEventsRepository
    lateinit var eventRepository: EventRepository
    lateinit var objectMapper: ObjectMapper
    lateinit var emailContentService: EmailContentService

    beforeContainer {
        campaignEventsRepository = mockk()
        eventRepository = mockk()
        objectMapper = ObjectMapper()
        emailContentService = EmailContentService(objectMapper, campaignEventsRepository, eventRepository)
    }

    given("EmailContentService") {
        val user = UserFixtures.giveMeOne().withUserAttributes(
            JsonFixtures.giveMeOne().withValue(
                """
                    {
                        "email": "test@example.com",
                        "name": "Test User"
                    }
                """.trimIndent()
            ).build()
        ).build()

        `when`("generating email content with no variables") {
            val notificationVariables = NotificationEmailTemplateVariablesModel("Subject", "Body", Variables())
            val result = emailContentService.genUserEmailContent(user, notificationVariables, null)

            then("should return NonContent") {
                result.shouldBeInstanceOf<NonContent>()
            }
        }

        `when`("generating email content with user variables only") {
            val variables = Variables(listOf("attribute_email", "attribute_name"))
            val notificationVariables = NotificationEmailTemplateVariablesModel("Subject", "Body", variables)

            val result = emailContentService.genUserEmailContent(user, notificationVariables, null)

            then("should return VariablesContent with user attributes") {
                result.shouldBeInstanceOf<VariablesContent>()
                val variablesContent = result
                variablesContent.variables["attribute_email"] shouldBe "test@example.com"
                variablesContent.variables["attribute_name"] shouldBe "Test User"
            }
        }

        `when`("generating email content with campaign") {
            val campaignId = 1L
            val variables = Variables(listOf("attribute_email"))
            val notificationVariables = NotificationEmailTemplateVariablesModel("Subject", "Body", variables)

            val campaignEvents = listOf(
                CampaignEvents.new(campaignId = campaignId, eventId = 10L),
                CampaignEvents.new(campaignId = campaignId, eventId = 11L)
            )

            val events = listOf(
                Event.new(
                    10L,
                    "Event1",
                    1L,
                    Properties(listOf(Property("eventProp1", "eventValue1"))),
                    LocalDateTime.now()
                ),
                Event.new(
                    11L,
                    "Event2",
                    2L,
                    Properties(listOf(Property("eventProp2", "eventValue2"))),
                    LocalDateTime.now()
                )
            )

            coEvery { campaignEventsRepository.findAllByCampaignId(campaignId) } returns campaignEvents
            coEvery { eventRepository.findAllByIdIn(listOf(10L, 11L)) } returns events

            val result = emailContentService.genUserEmailContent(user, notificationVariables, campaignId)

            then("should return VariablesContent with merged user and campaign variables") {
                result.shouldBeInstanceOf<VariablesContent>()
                val variablesContent = result
                variablesContent.variables["attribute_email"] shouldBe "test@example.com"
                variablesContent.variables["eventProp1"] shouldBe "eventValue1"
                variablesContent.variables["eventProp2"] shouldBe "eventValue2"
            }
        }

        `when`("generating email content with campaign but no campaign events") {
            val campaignId = 1L
            val variables = Variables(listOf("attribute_email"))
            val notificationVariables = NotificationEmailTemplateVariablesModel("Subject", "Body", variables)

            coEvery { campaignEventsRepository.findAllByCampaignId(campaignId) } returns emptyList()

            val result = emailContentService.genUserEmailContent(user, notificationVariables, campaignId)

            then("should return VariablesContent with only user attributes") {
                result.shouldBeInstanceOf<VariablesContent>()
                val variablesContent = result
                variablesContent.variables["attribute_email"] shouldBe "test@example.com"
                variablesContent.variables.size shouldBe 1
            }
        }

        `when`("generating email content with campaign but no events") {
            val campaignId = 1L
            val variables = Variables(listOf("attribute_email"))
            val notificationVariables = NotificationEmailTemplateVariablesModel("Subject", "Body", variables)

            val campaignEvents = listOf(
                CampaignEvents.new(campaignId = campaignId, eventId = 10L)
            )

            coEvery { campaignEventsRepository.findAllByCampaignId(campaignId) } returns campaignEvents
            coEvery { eventRepository.findAllByIdIn(listOf(10L)) } returns emptyList()

            val result = emailContentService.genUserEmailContent(user, notificationVariables, campaignId)

            then("should return VariablesContent with only user attributes") {
                result.shouldBeInstanceOf<VariablesContent>()
                val variablesContent = result
                variablesContent.variables["attribute_email"] shouldBe "test@example.com"
                variablesContent.variables.size shouldBe 1
            }
        }
    }
})
