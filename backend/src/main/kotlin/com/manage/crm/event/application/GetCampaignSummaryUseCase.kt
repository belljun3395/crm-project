package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetCampaignSummaryUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignSummaryUseCaseOut
import com.manage.crm.event.service.CampaignDashboardService
import org.springframework.stereotype.Component

@Component
class GetCampaignSummaryUseCase(
    private val campaignDashboardService: CampaignDashboardService
) {
    suspend fun execute(input: GetCampaignSummaryUseCaseIn): GetCampaignSummaryUseCaseOut {
        val summary = campaignDashboardService.getCampaignSummary(input.campaignId)

        return GetCampaignSummaryUseCaseOut(
            campaignId = summary.campaignId,
            totalEvents = summary.totalEvents,
            eventsLast24Hours = summary.eventsLast24Hours,
            eventsLast7Days = summary.eventsLast7Days,
            lastUpdated = summary.lastUpdated
        )
    }
}
