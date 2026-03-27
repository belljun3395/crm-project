package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignSummaryUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignSummaryUseCaseOut
import com.manage.crm.event.application.dto.toSummaryUseCaseOut
import com.manage.crm.event.domain.repository.CampaignDashboardMetricsRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * UC-CAMPAIGN-008
 * Reads campaign summary counters.
 *
 * Input: campaign id.
 * Success: returns total, 24h, and 7d event counts.
 */
@Component
class GetCampaignSummaryUseCase(
    private val campaignDashboardMetricsRepository: CampaignDashboardMetricsRepository
) {
    suspend fun execute(input: GetCampaignSummaryUseCaseIn): GetCampaignSummaryUseCaseOut {
        return getSummary(input.campaignId)
    }

    private suspend fun getSummary(campaignId: Long): GetCampaignSummaryUseCaseOut =
        LocalDateTime.now().let { now ->
            campaignDashboardMetricsRepository
                .getCampaignSummaryMetrics(
                    campaignId = campaignId,
                    last24Hours = now.minusHours(24),
                    last7Days = now.minusDays(7)
                )
                .toSummaryUseCaseOut(campaignId = campaignId, lastUpdated = now)
        }
}
