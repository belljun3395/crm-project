package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignDashboardUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignDashboardUseCaseOut
import com.manage.crm.event.application.dto.toDto
import com.manage.crm.event.domain.CampaignDashboardMetrics
import com.manage.crm.event.domain.repository.CampaignDashboardMetricsRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import java.time.LocalDateTime

data class CampaignDashboardSummary(
    val campaignId: Long,
    val totalEvents: Long,
    val eventsLast24Hours: Long,
    val eventsLast7Days: Long,
    val lastUpdated: LocalDateTime
)

/**
 * UC-CAMPAIGN-007
 * Reads campaign dashboard metrics and summary.
 *
 * Input: campaign id and optional time filters.
 * Success: returns persisted metrics plus aggregated summary.
 */
@Component
class GetCampaignDashboardUseCase(
    private val campaignDashboardMetricsRepository: CampaignDashboardMetricsRepository
) {
    suspend fun execute(input: GetCampaignDashboardUseCaseIn): GetCampaignDashboardUseCaseOut {
        val metrics = getMetrics(input)
        val summary = getSummary(input)

        return GetCampaignDashboardUseCaseOut(
            campaignId = input.campaignId,
            metrics = metrics.map { it.toDto() },
            summary = summary.toDto()
        )
    }

    private suspend fun getMetrics(input: GetCampaignDashboardUseCaseIn): List<CampaignDashboardMetrics> =
        if (input.timeWindowUnit != null) {
            val from = input.startTime ?: LocalDateTime.now().minusDays(7)
            campaignDashboardMetricsRepository.findByCampaignIdAndTimeWindowUnitAndTimeWindowStartAfter(
                input.campaignId,
                input.timeWindowUnit,
                from
            ).toList()
        } else if (input.startTime != null && input.endTime != null) {
            campaignDashboardMetricsRepository.findByCampaignIdAndTimeWindowStartBetween(
                input.campaignId,
                input.startTime,
                input.endTime
            ).toList()
        } else {
            campaignDashboardMetricsRepository.findAllByCampaignIdOrderByTimeWindowStartDesc(input.campaignId).toList()
        }

    private suspend fun getSummary(input: GetCampaignDashboardUseCaseIn): CampaignDashboardSummary {
        val now = LocalDateTime.now()
        val last24Hours = now.minusHours(24)
        val last7Days = now.minusDays(7)

        val summaryMetrics = campaignDashboardMetricsRepository.getCampaignSummaryMetrics(
            campaignId = input.campaignId,
            last24Hours = last24Hours,
            last7Days = last7Days
        )

        return CampaignDashboardSummary(
            campaignId = input.campaignId,
            totalEvents = summaryMetrics.totalEvents ?: 0L,
            eventsLast24Hours = summaryMetrics.eventsLast24Hours ?: 0L,
            eventsLast7Days = summaryMetrics.eventsLast7Days ?: 0L,
            lastUpdated = now
        )
    }
}
