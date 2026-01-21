package com.manage.crm.event.service

import com.manage.crm.event.domain.MetricType
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.event.domain.repository.CampaignDashboardMetricsRepository
import com.manage.crm.event.domain.repository.CampaignSummaryMetricsProjection
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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

            coEvery { streamService.publishEvent(any()) } returns Unit
            coEvery { streamService.getStreamLength(any()) } returns 50L
            coEvery { streamService.trimStream(any(), any()) } returns Unit
            coEvery {
                campaignDashboardMetricsRepository.upsertMetric(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns 1

            service.publishCampaignEvent(event)

            then("should publish event to stream") {
                coVerify(exactly = 1) { streamService.publishEvent(event) }
            }

            then("should upsert metrics for each time window unit") {
                coVerify(exactly = 1) {
                    campaignDashboardMetricsRepository.upsertMetric(
                        campaignId = 1L,
                        metricType = MetricType.EVENT_COUNT.name,
                        metricValue = 1L,
                        timeWindowStart = any(),
                        timeWindowEnd = any(),
                        timeWindowUnit = TimeWindowUnit.HOUR.name
                    )
                }
                coVerify(exactly = 1) {
                    campaignDashboardMetricsRepository.upsertMetric(
                        campaignId = 1L,
                        metricType = MetricType.EVENT_COUNT.name,
                        metricValue = 1L,
                        timeWindowStart = any(),
                        timeWindowEnd = any(),
                        timeWindowUnit = TimeWindowUnit.DAY.name
                    )
                }
            }
        }

        `when`("getCampaignSummary is called") {
            val campaignId = 1L

            val projection = CampaignSummaryMetricsProjection(
                totalEvents = 150L,
                eventsLast24Hours = 50L,
                eventsLast7Days = 100L
            )

            coEvery {
                campaignDashboardMetricsRepository.getCampaignSummaryMetrics(any(), any(), any())
            } returns projection

            val summary = service.getCampaignSummary(campaignId)

            then("should return summary with correct campaign id") {
                summary.campaignId shouldBe campaignId
            }

            then("should return correct event counts from projection") {
                summary.totalEvents shouldBe 150L
                summary.eventsLast24Hours shouldBe 50L
                summary.eventsLast7Days shouldBe 100L
            }

            then("should have a last updated timestamp") {
                summary.lastUpdated shouldNotBe null
            }
        }
    }
})
