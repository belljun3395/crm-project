package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignSummaryUseCaseIn
import com.manage.crm.event.domain.repository.CampaignDashboardMetricsRepository
import com.manage.crm.event.domain.repository.projection.CampaignSummaryMetricsProjection
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDateTime

class GetCampaignSummaryUseCaseTest :
    BehaviorSpec({
        lateinit var campaignDashboardMetricsRepository: CampaignDashboardMetricsRepository
        lateinit var getCampaignSummaryUseCase: GetCampaignSummaryUseCase

        beforeContainer {
            campaignDashboardMetricsRepository = mockk()
            getCampaignSummaryUseCase = GetCampaignSummaryUseCase(campaignDashboardMetricsRepository)
        }

        given("UC-CAMPAIGN-008 GetCampaignSummaryUseCase") {
            `when`("reading summary metrics") {
                val campaignId = 7L
                val last24Hours = slot<LocalDateTime>()
                val last7Days = slot<LocalDateTime>()
                coEvery {
                    campaignDashboardMetricsRepository.getCampaignSummaryMetrics(
                        campaignId = campaignId,
                        last24Hours = capture(last24Hours),
                        last7Days = capture(last7Days),
                    )
                } returns
                    CampaignSummaryMetricsProjection(
                        totalEvents = 100L,
                        eventsLast24Hours = 10L,
                        eventsLast7Days = 70L,
                    )

                val result = getCampaignSummaryUseCase.execute(GetCampaignSummaryUseCaseIn(campaignId))

                then("should map summary projection to use case output") {
                    result.campaignId shouldBe campaignId
                    result.totalEvents shouldBe 100L
                    result.eventsLast24Hours shouldBe 10L
                    result.eventsLast7Days shouldBe 70L
                    result.lastUpdated shouldNotBe null
                }

                then("should request 24h and 7d ranges in correct order") {
                    last7Days.captured.isBefore(last24Hours.captured) shouldBe true
                }
            }
        }
    })
