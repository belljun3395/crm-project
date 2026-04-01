package com.manage.crm.event.application

import com.manage.crm.event.application.dto.CampaignPropertyUseCaseDto
import com.manage.crm.event.application.dto.GetCampaignUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignUseCaseOut
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.CampaignSegmentsRepository
import com.manage.crm.support.exception.NotFoundByIdException
import org.springframework.stereotype.Component

/**
 * UC-CAMPAIGN-003
 * Reads a campaign with attached segment ids.
 *
 * Input: campaign id.
 * Success: returns campaign detail.
 * Failure: throws when campaign does not exist.
 */
@Component
class GetCampaignUseCase(
    private val campaignRepository: CampaignRepository,
    private val campaignSegmentsRepository: CampaignSegmentsRepository,
) {
    suspend fun execute(input: GetCampaignUseCaseIn): GetCampaignUseCaseOut {
        val campaign =
            campaignRepository.findById(input.campaignId)
                ?: throw NotFoundByIdException("Campaign", input.campaignId)
        val segmentIds =
            campaignSegmentsRepository
                .findAllByCampaignId(input.campaignId)
                .map { it.segmentId }

        return GetCampaignUseCaseOut(
            id = campaign.id ?: input.campaignId,
            name = campaign.name,
            properties = campaign.properties.value.map { CampaignPropertyUseCaseDto(it.key, it.value) },
            segmentIds = segmentIds,
            createdAt = campaign.createdAt,
        )
    }
}
