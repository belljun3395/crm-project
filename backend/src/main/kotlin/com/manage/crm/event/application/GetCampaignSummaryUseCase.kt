package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignSummaryUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignSummaryUseCaseOut
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
        val summary = getSummary(input.campaignId)

        return GetCampaignSummaryUseCaseOut(
            campaignId = summary.campaignId,
            totalEvents = summary.totalEvents,
            eventsLast24Hours = summary.eventsLast24Hours,
            eventsLast7Days = summary.eventsLast7Days,
            lastUpdated = summary.lastUpdated
        )
    }

    private suspend fun getSummary(campaignId: Long): CampaignDashboardSummary {
        val now = LocalDateTime.now()
        val last24Hours = now.minusHours(24)
        val last7Days = now.minusDays(7)

        val summaryMetrics = campaignDashboardMetricsRepository.getCampaignSummaryMetrics(
            campaignId = campaignId,
            last24Hours = last24Hours,
            last7Days = last7Days
        )

        return CampaignDashboardSummary(
            campaignId = campaignId,
            totalEvents = summaryMetrics.totalEvents ?: 0L,
            eventsLast24Hours = summaryMetrics.eventsLast24Hours ?: 0L,
            eventsLast7Days = summaryMetrics.eventsLast7Days ?: 0L,
            lastUpdated = now
        )
    }
}
