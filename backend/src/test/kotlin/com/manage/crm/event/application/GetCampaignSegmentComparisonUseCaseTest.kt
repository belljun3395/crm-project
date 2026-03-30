package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseIn
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.PropertiesFixtures
import com.manage.crm.event.service.CampaignEventsService
import com.manage.crm.segment.domain.Segment
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.segment.service.SegmentTargetingService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDateTime

class GetCampaignSegmentComparisonUseCaseTest : BehaviorSpec({
    lateinit var campaignEventsService: CampaignEventsService
    lateinit var segmentRepository: SegmentRepository
    lateinit var segmentTargetingService: SegmentTargetingService
    lateinit var getCampaignSegmentComparisonUseCase: GetCampaignSegmentComparisonUseCase

    beforeContainer {
        campaignEventsService = mockk()
        segmentRepository = mockk()
        segmentTargetingService = mockk()
        getCampaignSegmentComparisonUseCase = GetCampaignSegmentComparisonUseCase(
            campaignEventsService = campaignEventsService,
            segmentRepository = segmentRepository,
            segmentTargetingService = segmentTargetingService
        )
    }

    given("UC-CAMPAIGN-010: GetCampaignSegmentComparisonUseCase") {
        `when`("segmentIds is empty or all invalid (<= 0)") {
            then("throws IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    getCampaignSegmentComparisonUseCase.execute(
                        GetCampaignSegmentComparisonUseCaseIn(
                            campaignId = 1L,
                            segmentIds = listOf(0L, -1L),
                            eventName = null,
                            startTime = null,
                            endTime = null
                        )
                    )
                }
            }
        }

        `when`("valid segments with events") {
            val campaignId = 10L
            val now = LocalDateTime.now()
            val events = listOf(
                segEvent(1L, 101L, "purchase", now),
                segEvent(2L, 102L, "purchase", now),
                segEvent(3L, 103L, "purchase", now)
            )

            coEvery { campaignEventsService.findCampaignEvents(campaignId, null, null) } returns events
            coEvery { segmentRepository.findById(1L) } returns Segment.new(1L, "seg-a", null, true)
            coEvery { segmentRepository.findById(2L) } returns Segment.new(2L, "seg-b", null, true)
            coEvery { segmentTargetingService.resolveUserIds(1L, campaignId) } returns listOf(101L, 102L)
            coEvery { segmentTargetingService.resolveUserIds(2L, campaignId) } returns listOf(103L)

            val result = getCampaignSegmentComparisonUseCase.execute(
                GetCampaignSegmentComparisonUseCaseIn(campaignId, listOf(1L, 2L), null, null, null)
            )

            then("returns metrics for each segment") {
                result.segmentMetrics.size shouldBe 2
            }

            then("segment metrics reflect correct event and user counts") {
                val segA = result.segmentMetrics.first { it.segmentId == 1L }
                segA.targetUserCount shouldBe 2
                segA.eventUserCount shouldBe 2
                segA.eventCount shouldBe 2

                val segB = result.segmentMetrics.first { it.segmentId == 2L }
                segB.targetUserCount shouldBe 1
                segB.eventUserCount shouldBe 1
                segB.eventCount shouldBe 1
            }

            then("results are sorted descending by conversion rate") {
                val rates = result.segmentMetrics.map { it.conversionRate }
                rates shouldBe rates.sortedDescending()
            }
        }

        `when`("eventName filter is applied") {
            val campaignId = 11L
            val now = LocalDateTime.now()
            val events = listOf(
                segEvent(1L, 101L, "purchase", now),
                segEvent(2L, 101L, "view", now)
            )

            coEvery { campaignEventsService.findCampaignEvents(campaignId, null, null) } returns events
            coEvery { segmentRepository.findById(1L) } returns Segment.new(1L, "seg-a", null, true)
            coEvery { segmentTargetingService.resolveUserIds(1L, campaignId) } returns listOf(101L)

            val result = getCampaignSegmentComparisonUseCase.execute(
                GetCampaignSegmentComparisonUseCaseIn(campaignId, listOf(1L), "purchase", null, null)
            )

            then("only purchase events are counted") {
                result.segmentMetrics.first().eventCount shouldBe 1
                result.eventName shouldBe "purchase"
            }
        }

        `when`("segment no longer exists (deleted segment)") {
            val campaignId = 13L
            val now = LocalDateTime.now()
            val events = listOf(segEvent(1L, 101L, "click", now))

            coEvery { campaignEventsService.findCampaignEvents(campaignId, null, null) } returns events
            coEvery { segmentRepository.findById(99L) } returns null
            coEvery { segmentTargetingService.resolveUserIds(99L, campaignId) } returns listOf(101L)

            val result = getCampaignSegmentComparisonUseCase.execute(
                GetCampaignSegmentComparisonUseCaseIn(campaignId, listOf(99L), null, null, null)
            )

            then("segmentName is null for missing segment") {
                result.segmentMetrics.first().segmentName shouldBe null
                result.segmentMetrics.first().segmentId shouldBe 99L
            }
        }

        `when`("startTime and endTime filter are provided") {
            val campaignId = 14L
            val start = LocalDateTime.of(2026, 1, 1, 0, 0)
            val end = LocalDateTime.of(2026, 1, 31, 0, 0)

            coEvery { campaignEventsService.findCampaignEvents(campaignId, start, end) } returns emptyList()
            coEvery { segmentRepository.findById(1L) } returns Segment.new(1L, "s1", null, true)
            coEvery { segmentTargetingService.resolveUserIds(1L, campaignId) } returns emptyList()

            getCampaignSegmentComparisonUseCase.execute(
                GetCampaignSegmentComparisonUseCaseIn(campaignId, listOf(1L), null, start, end)
            )

            then("time range is passed to campaignEventsService") {
                io.mockk.coVerify(exactly = 1) {
                    campaignEventsService.findCampaignEvents(campaignId, start, end)
                }
            }
        }

        `when`("no segment target users have matching events") {
            val campaignId = 12L
            val now = LocalDateTime.now()
            val events = listOf(
                segEvent(1L, 999L, "purchase", now) // userId 999 not in segment
            )

            coEvery { campaignEventsService.findCampaignEvents(campaignId, null, null) } returns events
            coEvery { segmentRepository.findById(1L) } returns Segment.new(1L, "empty-seg", null, true)
            coEvery { segmentTargetingService.resolveUserIds(1L, campaignId) } returns listOf(101L, 102L)

            val result = getCampaignSegmentComparisonUseCase.execute(
                GetCampaignSegmentComparisonUseCaseIn(campaignId, listOf(1L), null, null, null)
            )

            then("conversion rate is 0") {
                result.segmentMetrics.first().conversionRate shouldBe 0.0
                result.segmentMetrics.first().eventUserCount shouldBe 0
            }
        }
    }
})

private fun segEvent(id: Long, userId: Long, name: String, createdAt: LocalDateTime): Event =
    Event.new(id, name, userId, PropertiesFixtures.giveMeOneEventProperties(), createdAt)
