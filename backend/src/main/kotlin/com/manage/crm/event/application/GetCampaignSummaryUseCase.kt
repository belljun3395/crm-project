package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignSummaryUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignSummaryUseCaseOut
import com.manage.crm.event.domain.repository.CampaignDashboardMetricsRepository
import com.manage.crm.event.domain.repository.projection.CampaignSummaryMetricsProjection
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
    private val campaignDashboardMetricsRepository: CampaignDashboardMetricsRepository,
) {
    suspend fun execute(input: GetCampaignSummaryUseCaseIn): GetCampaignSummaryUseCaseOut {
        val now = LocalDateTime.now()
        val summary = getSummary(input.campaignId, now)

        return GetCampaignSummaryUseCaseOut(
            campaignId = input.campaignId,
            totalEvents = summary.totalEvents,
            eventsLast24Hours = summary.eventsLast24Hours,
            eventsLast7Days = summary.eventsLast7Days,
            lastUpdated = now,
        )
    }

    private suspend fun getSummary(
        campaignId: Long,
        now: LocalDateTime,
    ): CampaignSummaryMetricsProjection =
        campaignDashboardMetricsRepository
            .getCampaignSummaryMetrics(
                campaignId = campaignId,
                last24Hours = now.minusHours(24),
                last7Days = now.minusDays(7),
            )
}
