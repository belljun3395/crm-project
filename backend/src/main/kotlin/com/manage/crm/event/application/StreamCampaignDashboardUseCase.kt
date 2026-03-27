package com.manage.crm.event.application

import com.manage.crm.event.application.dto.StreamCampaignDashboardUseCaseIn
import com.manage.crm.event.stream.CampaignDashboardStreamManager
import org.springframework.stereotype.Service

/**
 * UC-CAMPAIGN-006
 * Opens campaign dashboard stream from Redis Stream source.
 *
 * Input: campaign id and optional last-event id for resume.
 * Success: returns campaign event stream.
 */
@Service
class StreamCampaignDashboardUseCase(
    private val campaignDashboardStreamManager: CampaignDashboardStreamManager
) {
    fun execute(input: StreamCampaignDashboardUseCaseIn) =
        campaignDashboardStreamManager.streamEvents(
            campaignId = input.campaignId,
            lastEventId = input.lastEventId
        )
}
