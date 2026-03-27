package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetStreamStatusUseCaseIn
import com.manage.crm.event.application.dto.GetStreamStatusUseCaseOut
import com.manage.crm.event.stream.CampaignDashboardStreamManager
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * UC-CAMPAIGN-011
 * Reads current stream length for campaign dashboard stream.
 *
 * Input: campaign id.
 * Success: returns stream length with checked timestamp.
 */
@Component
class GetCampaignDashboardStreamStatusUseCase(
    private val campaignDashboardStreamManager: CampaignDashboardStreamManager
) {
    suspend fun execute(input: GetStreamStatusUseCaseIn): GetStreamStatusUseCaseOut {
        val streamLength = campaignDashboardStreamManager.getStreamLength(input.campaignId)

        return GetStreamStatusUseCaseOut(
            campaignId = input.campaignId,
            streamLength = streamLength,
            checkedAt = LocalDateTime.now()
        )
    }
}
