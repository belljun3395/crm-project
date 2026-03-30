package com.manage.crm.event.service

import com.manage.crm.event.domain.CampaignFixtures
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.PropertiesFixtures
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.support.exception.NotFoundByIdException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class CampaignEventsServiceTest : BehaviorSpec({
    lateinit var campaignRepository: CampaignRepository
    lateinit var eventRepository: EventRepository
    lateinit var campaignEventsRepository: CampaignEventsRepository
    lateinit var campaignEventsService: CampaignEventsService

    beforeContainer {
        campaignRepository = mockk()
        eventRepository = mockk()
        campaignEventsRepository = mockk()
        campaignEventsService = CampaignEventsService(
            campaignRepository = campaignRepository,
            eventRepository = eventRepository,
            campaignEventsRepository = campaignEventsRepository
        )
    }

    given("CampaignEventsService") {
        `when`("finding all events by campaign id") {
            val campaignId = 11L
            val eventIds = listOf(1L, 2L)
            val events = listOf(
                Event.new(1L, "e1", 101L, PropertiesFixtures.giveMeOne().buildEvent(), LocalDateTime.now()),
                Event.new(2L, "e2", 102L, PropertiesFixtures.giveMeOne().buildEvent(), LocalDateTime.now())
            )
            coEvery { campaignEventsRepository.findEventIdsByCampaignId(campaignId) } returns eventIds
            coEvery { eventRepository.findAllByIdIn(eventIds) } returns events

            val result = campaignEventsService.findAllEventsByCampaignId(campaignId)

            then("should hydrate events from relation ids") {
                result.shouldContainExactly(events)
            }
        }

        `when`("finding campaign events with full range") {
            val campaignId = 12L
            val start = LocalDateTime.of(2026, 3, 1, 0, 0)
            val end = LocalDateTime.of(2026, 3, 2, 0, 0)
            val eventIds = listOf(10L, 11L)
            val events = listOf(
                Event.new(10L, "a", 1L, PropertiesFixtures.giveMeOne().buildEvent(), start.plusHours(1)),
                Event.new(11L, "b", 2L, PropertiesFixtures.giveMeOne().buildEvent(), start.plusHours(2))
            )
            coEvery { campaignRepository.findById(campaignId) } returns CampaignFixtures.giveMeOne().withId(campaignId).build()
            coEvery {
                campaignEventsRepository.findEventIdsByCampaignIdAndCreatedAtRange(
                    campaignId,
                    start,
                    end
                )
            } returns eventIds
            coEvery { eventRepository.findAllByIdIn(eventIds) } returns events

            val result = campaignEventsService.findCampaignEvents(campaignId, start, end)

            then("should use range query without extra filtering") {
                result.shouldContainExactly(events)
                coVerify(exactly = 1) {
                    campaignEventsRepository.findEventIdsByCampaignIdAndCreatedAtRange(
                        campaignId,
                        start,
                        end
                    )
                }
                coVerify(exactly = 0) { campaignEventsRepository.findEventIdsByCampaignId(campaignId) }
            }
        }

        `when`("finding campaign events with partial range") {
            val campaignId = 13L
            val start = LocalDateTime.of(2026, 3, 1, 0, 0)
            val allEventIds = listOf(20L, 21L, 22L)
            val events = listOf(
                Event.new(20L, "old", 1L, PropertiesFixtures.giveMeOne().buildEvent(), start.minusMinutes(1)),
                Event.new(21L, "ok", 2L, PropertiesFixtures.giveMeOne().buildEvent(), start.plusMinutes(1)),
                Event.new(22L, "null-created-at", 3L, PropertiesFixtures.giveMeOne().buildEvent(), start).apply { createdAt = null }
            )
            coEvery { campaignRepository.findById(campaignId) } returns CampaignFixtures.giveMeOne().withId(campaignId).build()
            coEvery { campaignEventsRepository.findEventIdsByCampaignId(campaignId) } returns allEventIds
            coEvery { eventRepository.findAllByIdIn(allEventIds) } returns events

            val result = campaignEventsService.findCampaignEvents(campaignId, start, null)

            then("should apply null-safe in-memory bound filtering") {
                result shouldHaveSize 1
                result.first().name shouldBe "ok"
            }
        }

        `when`("campaign does not exist") {
            val campaignId = 14L
            coEvery { campaignRepository.findById(campaignId) } returns null

            then("should throw not found by id") {
                shouldThrow<NotFoundByIdException> {
                    campaignEventsService.findCampaignEvents(campaignId, null, null)
                }
            }
        }
    }
})
