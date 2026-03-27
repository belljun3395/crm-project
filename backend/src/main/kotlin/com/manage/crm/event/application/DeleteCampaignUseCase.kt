package com.manage.crm.event.application

import com.manage.crm.event.application.dto.DeleteCampaignUseCaseIn
import com.manage.crm.event.application.dto.DeleteCampaignUseCaseOut
import com.manage.crm.event.domain.cache.CampaignCacheManager
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.CampaignSegmentsRepository
import com.manage.crm.support.exception.NotFoundByIdException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * UC-CAMPAIGN-005
 * Deletes campaign and its segment mappings.
 *
 * Input: campaign id.
 * Success: campaign and related mappings are removed.
 * Failure: throws when campaign does not exist.
 */
@Service
class DeleteCampaignUseCase(
    private val campaignRepository: CampaignRepository,
    private val campaignSegmentsRepository: CampaignSegmentsRepository,
    private val campaignCacheManager: CampaignCacheManager
) {
    @Transactional
    suspend fun execute(input: DeleteCampaignUseCaseIn): DeleteCampaignUseCaseOut {
        val campaign = campaignRepository.findById(input.campaignId)
            ?: throw NotFoundByIdException("Campaign", input.campaignId)

        campaignSegmentsRepository.deleteAllByCampaignId(input.campaignId)
        campaignRepository.deleteById(input.campaignId)
        campaignCacheManager.evict(input.campaignId, campaign.name)

        return DeleteCampaignUseCaseOut(success = true)
    }
}
