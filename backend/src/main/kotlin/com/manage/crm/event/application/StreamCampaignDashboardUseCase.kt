package com.manage.crm.event.application

import com.manage.crm.event.application.dto.StreamCampaignDashboardUseCaseIn
import com.manage.crm.event.stream.CampaignDashboardStreamManager
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * UC-CAMPAIGN-006
 * Opens campaign dashboard stream from Redis Stream source.
 *
 * Input: campaign id and optional last-event id for resume.
 * Success: returns campaign event stream.
 */
@Component
class StreamCampaignDashboardUseCase(
    private val campaignDashboardStreamManager: CampaignDashboardStreamManager
) {
    fun execute(input: StreamCampaignDashboardUseCaseIn) =
        campaignDashboardStreamManager.streamEvents(
            campaignId = input.campaignId,
            duration = Duration.ofSeconds(input.durationSeconds),
            lastEventId = input.lastEventId
        )
}
