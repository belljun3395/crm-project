package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignDashboardUseCaseIn
import com.manage.crm.event.domain.CampaignDashboardMetrics
import com.manage.crm.event.domain.MetricType
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.event.domain.repository.CampaignDashboardMetricsRepository
import com.manage.crm.event.domain.repository.projection.CampaignSummaryMetricsProjection
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class GetCampaignDashboardUseCaseTest : BehaviorSpec({
    lateinit var campaignDashboardMetricsRepository: CampaignDashboardMetricsRepository
    lateinit var getCampaignDashboardUseCase: GetCampaignDashboardUseCase

    beforeContainer {
        campaignDashboardMetricsRepository = mockk()
        getCampaignDashboardUseCase = GetCampaignDashboardUseCase(campaignDashboardMetricsRepository)
    }

    given("UC-CAMPAIGN-007 GetCampaignDashboardUseCase") {
        `when`("time window filter is provided") {
            val campaignId = 21L
            val from = LocalDateTime.of(2026, 3, 1, 0, 0)
            val metric = CampaignDashboardMetrics.new(
                campaignId = campaignId,
                metricType = MetricType.EVENT_COUNT,
                metricValue = 5,
                timeWindowStart = from,
                timeWindowEnd = from.plusHours(1),
                timeWindowUnit = TimeWindowUnit.HOUR
            ).apply {
                id = 100L
            }
            every {
                campaignDashboardMetricsRepository.findByCampaignIdAndTimeWindowUnitAndTimeWindowStartAfter(
                    campaignId,
                    TimeWindowUnit.HOUR,
                    from
                )
            } returns flowOf(metric)
            every {
                campaignDashboardMetricsRepository.findByCampaignIdAndTimeWindowStartBetween(any(), any(), any())
            } returns emptyFlow()
            every {
                campaignDashboardMetricsRepository.findAllByCampaignIdOrderByTimeWindowStartDesc(any())
            } returns emptyFlow()
            coEvery {
                campaignDashboardMetricsRepository.getCampaignSummaryMetrics(
                    campaignId = campaignId,
                    last24Hours = any(),
                    last7Days = any()
                )
            } returns CampaignSummaryMetricsProjection(
                totalEvents = 50L,
                eventsLast24Hours = 20L,
                eventsLast7Days = 40L
            )

            val result = getCampaignDashboardUseCase.execute(
                GetCampaignDashboardUseCaseIn(
                    campaignId = campaignId,
                    startTime = from,
                    timeWindowUnit = TimeWindowUnit.HOUR
                )
            )

            then("should read metrics from time-unit repository query") {
                result.metrics shouldHaveSize 1
                result.metrics.first().id shouldBe 100L
                result.metrics.first().metricValue shouldBe 5L
                result.summary.totalEvents shouldBe 50L
                verify(exactly = 1) {
                    campaignDashboardMetricsRepository.findByCampaignIdAndTimeWindowUnitAndTimeWindowStartAfter(
                        campaignId,
                        TimeWindowUnit.HOUR,
                        from
                    )
                }
            }
        }

        `when`("no filter is provided") {
            val campaignId = 22L
            val metric = CampaignDashboardMetrics.new(
                campaignId = campaignId,
                metricType = MetricType.EVENT_COUNT,
                metricValue = 3,
                timeWindowStart = LocalDateTime.now().minusHours(1),
                timeWindowEnd = LocalDateTime.now(),
                timeWindowUnit = TimeWindowUnit.HOUR
            )
            every {
                campaignDashboardMetricsRepository.findAllByCampaignIdOrderByTimeWindowStartDesc(campaignId)
            } returns flowOf(metric)
            coEvery {
                campaignDashboardMetricsRepository.getCampaignSummaryMetrics(
                    campaignId = campaignId,
                    last24Hours = any(),
                    last7Days = any()
                )
            } returns CampaignSummaryMetricsProjection(
                totalEvents = 3L,
                eventsLast24Hours = 3L,
                eventsLast7Days = 3L
            )

            val result = getCampaignDashboardUseCase.execute(GetCampaignDashboardUseCaseIn(campaignId = campaignId))

            then("should fallback to latest metrics query") {
                result.metrics shouldHaveSize 1
                result.summary.eventsLast24Hours shouldBe 3L
                verify(exactly = 1) {
                    campaignDashboardMetricsRepository.findAllByCampaignIdOrderByTimeWindowStartDesc(campaignId)
                }
            }
        }
    }
})
