package com.manage.crm.event.application

import com.manage.crm.event.application.dto.CampaignListItemUseCaseDto
import com.manage.crm.event.application.dto.ListCampaignsUseCaseIn
import com.manage.crm.event.application.dto.ListCampaignsUseCaseOut
import com.manage.crm.event.domain.repository.CampaignRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

/**
 * UC-CAMPAIGN-002
 * Lists campaigns in reverse chronological order.
 *
 * Input: max item limit.
 * Success: returns campaign ids, names, and created timestamps.
 */
@Service
class ListCampaignsUseCase(
    private val campaignRepository: CampaignRepository
) {
    suspend fun execute(input: ListCampaignsUseCaseIn): ListCampaignsUseCaseOut {
        val campaigns = loadRecentCampaigns(input.limit)
        return ListCampaignsUseCaseOut(campaigns = campaigns)
    }

    private suspend fun loadRecentCampaigns(limit: Int): List<CampaignListItemUseCaseDto> {
        val normalizedLimit = limit.coerceIn(1, 1000)
        return campaignRepository.findRecentCampaigns(normalizedLimit)
            .toList()
            .mapNotNull { campaign ->
                val campaignId = campaign.id ?: return@mapNotNull null
                CampaignListItemUseCaseDto(
                    id = campaignId,
                    name = campaign.name,
                    createdAt = campaign.createdAt
                )
            }
    }
}
