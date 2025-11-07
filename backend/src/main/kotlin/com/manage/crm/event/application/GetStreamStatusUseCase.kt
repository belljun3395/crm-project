package com.manage.crm.event.application

import com.manage.crm.event.application.dto.GetStreamStatusUseCaseIn
import com.manage.crm.event.application.dto.GetStreamStatusUseCaseOut
import com.manage.crm.event.service.CampaignDashboardService
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class GetStreamStatusUseCase(
    private val campaignDashboardService: CampaignDashboardService
) {
    suspend fun execute(input: GetStreamStatusUseCaseIn): GetStreamStatusUseCaseOut {
        val streamLength = campaignDashboardService.getStreamLength(input.campaignId)

        return GetStreamStatusUseCaseOut(
            campaignId = input.campaignId,
            streamLength = streamLength,
            checkedAt = LocalDateTime.now()
        )
    }
}
