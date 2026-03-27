package com.manage.crm.event.application

import com.manage.crm.event.application.dto.DeleteCampaignUseCaseIn
import com.manage.crm.event.application.dto.DeleteCampaignUseCaseOut
import com.manage.crm.event.domain.cache.CampaignCacheManager
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.CampaignSegmentsRepository
import com.manage.crm.event.stream.CampaignDashboardStreamManager
import com.manage.crm.event.stream.CampaignStreamRegistryManager
import com.manage.crm.support.exception.NotFoundByIdException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * UC-CAMPAIGN-005
 * Deletes campaign and its segment mappings.
 *
 * Input: campaign id.
 * Success: campaign and related mappings are removed.
 * Failure: throws when campaign does not exist.
 */
@Component
class DeleteCampaignUseCase(
    private val campaignRepository: CampaignRepository,
    private val campaignSegmentsRepository: CampaignSegmentsRepository,
    private val campaignCacheManager: CampaignCacheManager,
    private val campaignDashboardStreamManager: CampaignDashboardStreamManager,
    private val campaignStreamRegistryManager: CampaignStreamRegistryManager
) {
    @Transactional
    suspend fun execute(input: DeleteCampaignUseCaseIn): DeleteCampaignUseCaseOut {
        val campaign = campaignRepository.findById(input.campaignId)
            ?: throw NotFoundByIdException("Campaign", input.campaignId)

        campaignSegmentsRepository.deleteAllByCampaignId(input.campaignId)
        campaignRepository.deleteById(input.campaignId)
        // TODO(transaction-consistency): move external side effects to afterCommit (or outbox),
        // because cache/stream updates are not rolled back with DB transaction failure.
        campaignCacheManager.evict(input.campaignId, campaign.name)
        campaignDashboardStreamManager.deleteStream(input.campaignId)
        campaignStreamRegistryManager.unregisterCampaign(input.campaignId)

        return DeleteCampaignUseCaseOut(success = true)
    }
}
