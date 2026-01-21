package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignDashboardUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignDashboardUseCaseOut
import com.manage.crm.event.application.dto.toDto
import com.manage.crm.event.domain.CampaignDashboardMetrics
import com.manage.crm.event.service.CampaignDashboardService
import com.manage.crm.event.service.dto.CampaignDashboardSummary
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class GetCampaignDashboardUseCase(
    private val campaignDashboardService: CampaignDashboardService
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
            campaignDashboardService.getMetricsByTimeUnit(
                campaignId = input.campaignId,
                timeWindowUnit = input.timeWindowUnit,
                from = from
            )
        } else if (input.startTime != null && input.endTime != null) {
            campaignDashboardService.getMetricsForCampaign(
                campaignId = input.campaignId,
                startTime = input.startTime,
                endTime = input.endTime
            )
        } else {
            campaignDashboardService.getAllMetricsForCampaign(input.campaignId)
        }

    private suspend fun getSummary(input: GetCampaignDashboardUseCaseIn): CampaignDashboardSummary =
        campaignDashboardService.getCampaignSummary(input.campaignId)
}
