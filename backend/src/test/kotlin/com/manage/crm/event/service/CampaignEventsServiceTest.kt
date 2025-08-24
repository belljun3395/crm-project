package com.manage.crm.event.service

import com.manage.crm.event.domain.CampaignEvents
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDateTime

class CampaignEventsServiceTest : BehaviorSpec({
    lateinit var campaignEventsRepository: CampaignEventsRepository
    lateinit var eventRepository: EventRepository
    lateinit var campaignEventsService: CampaignEventsService

    beforeContainer {
        campaignEventsRepository = mockk()
        eventRepository = mockk()
        campaignEventsService = CampaignEventsService(campaignEventsRepository, eventRepository)
    }

    given("CampaignEventsService") {
        `when`("finding all events by campaign id") {
            val campaignId = 1L
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

            val result = campaignEventsService.findAllEventsByCampaignId(campaignId)

            then("should return events") {
                result.size shouldBe 2
                result[0].id shouldBe 10L
                result[1].id shouldBe 11L
            }
        }

        `when`("getting user ids by campaign id") {
            val campaignId = 1L
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

            val result = campaignEventsService.getAllEventUserIdsByCampaignId(campaignId)

            then("should return user ids as set") {
                result shouldBe setOf(1L, 2L)
            }
        }

        `when`("getting user ids by campaign id with no campaign events") {
            val campaignId = 1L

            coEvery { campaignEventsRepository.findAllByCampaignId(campaignId) } returns emptyList()
            coEvery { eventRepository.findAllByIdIn(emptyList()) } returns emptyList()

            val result = campaignEventsService.getAllEventUserIdsByCampaignId(campaignId)

            then("should return empty set") {
                result shouldBe emptySet()
            }
        }

        `when`("getting user ids by campaign id with no events") {
            val campaignId = 1L
            val campaignEvents = listOf(
                CampaignEvents.new(campaignId = campaignId, eventId = 10L)
            )

            coEvery { campaignEventsRepository.findAllByCampaignId(campaignId) } returns campaignEvents
            coEvery { eventRepository.findAllByIdIn(listOf(10L)) } returns emptyList()

            val result = campaignEventsService.getAllEventUserIdsByCampaignId(campaignId)

            then("should return empty set") {
                result shouldBe emptySet()
            }
        }
    }
})