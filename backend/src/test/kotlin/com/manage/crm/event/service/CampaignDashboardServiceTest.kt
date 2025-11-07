package com.manage.crm.event.service

import com.manage.crm.event.domain.CampaignDashboardMetrics
import com.manage.crm.event.domain.MetricType
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.event.domain.repository.CampaignDashboardMetricsRepository
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class CampaignDashboardServiceTest : BehaviorSpec({
    lateinit var campaignDashboardMetricsRepository: CampaignDashboardMetricsRepository
    lateinit var campaignEventsRepository: CampaignEventsRepository
    lateinit var streamService: CampaignDashboardStreamService
    lateinit var service: CampaignDashboardService

    beforeContainer {
        campaignDashboardMetricsRepository = mockk()
        campaignEventsRepository = mockk()
        streamService = mockk()
        service = CampaignDashboardService(
            campaignDashboardMetricsRepository,
            campaignEventsRepository,
            streamService
        )
    }

    given("CampaignDashboardService") {
        `when`("publishCampaignEvent is called") {
            val event = CampaignDashboardEvent(
                campaignId = 1L,
                eventId = 100L,
                userId = 50L,
                eventName = "test_event",
                timestamp = LocalDateTime.now()
            )

            val metricSlot = slot<CampaignDashboardMetrics>()

            coEvery { streamService.publishEvent(any()) } returns Unit
            coEvery { streamService.getStreamLength(any()) } returns 50L
            coEvery { streamService.trimStream(any(), any()) } returns Unit
            coEvery {
                campaignDashboardMetricsRepository.incrementMetricValue(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns 0 // No rows updated, will create new metric
            coEvery { campaignDashboardMetricsRepository.save(capture(metricSlot)) } answers { metricSlot.captured }

            service.publishCampaignEvent(event)

            then("should publish event to stream") {
                coVerify(exactly = 1) { streamService.publishEvent(event) }
            }

            then("should save metrics for each time window unit") {
                // HOUR and DAY metrics are created
                coVerify(exactly = 1) {
                    campaignDashboardMetricsRepository.save(
                        match { it.timeWindowUnit == TimeWindowUnit.HOUR }
                    )
                }
                coVerify(exactly = 1) {
                    campaignDashboardMetricsRepository.save(
                        match { it.timeWindowUnit == TimeWindowUnit.DAY }
                    )
                }
            }
        }

        `when`("getCampaignSummary is called") {
            val campaignId = 1L
            val now = LocalDateTime.now()

            val metrics = listOf(
                CampaignDashboardMetrics.new(
                    campaignId = campaignId,
                    metricType = MetricType.EVENT_COUNT,
                    metricValue = 100L,
                    timeWindowStart = now.minusDays(1),
                    timeWindowEnd = now,
                    timeWindowUnit = TimeWindowUnit.DAY
                ),
                CampaignDashboardMetrics.new(
                    campaignId = campaignId,
                    metricType = MetricType.EVENT_COUNT,
                    metricValue = 50L,
                    timeWindowStart = now.minusHours(1),
                    timeWindowEnd = now,
                    timeWindowUnit = TimeWindowUnit.HOUR
                )
            )

            coEvery {
                campaignDashboardMetricsRepository.findAllByCampaignIdOrderByTimeWindowStartDesc(campaignId)
            } returns flowOf(*metrics.toTypedArray())

            val summary = service.getCampaignSummary(campaignId)

            then("should return summary with correct campaign id") {
                summary.campaignId shouldBe campaignId
            }

            then("should calculate total events correctly using only HOUR metrics") {
                // Only HOUR metrics are counted to avoid double counting
                summary.totalEvents shouldBe 50L
            }

            then("should have a last updated timestamp") {
                summary.lastUpdated shouldNotBe null
            }
        }
    }
})
