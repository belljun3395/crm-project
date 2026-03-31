package com.manage.crm.event.application

import com.manage.crm.event.application.dto.CampaignPropertyUseCaseDto
import com.manage.crm.event.application.dto.UpdateCampaignUseCaseIn
import com.manage.crm.event.application.dto.UpdateCampaignUseCaseOut
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.CampaignSegments
import com.manage.crm.event.domain.cache.CampaignCacheManager
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.CampaignSegmentsRepository
import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.support.exception.AlreadyExistsException
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.transactional.TransactionSynchronizationTemplate
import kotlinx.coroutines.Dispatchers
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * UC-CAMPAIGN-004
 * Updates campaign name/properties/segments and refreshes cache.
 *
 * Input: campaign id, new campaign fields, segment ids.
 * Success: returns updated campaign detail.
 * Failure: throws when campaign/segment is missing or name is duplicated.
 */
@Component
class UpdateCampaignUseCase(
    private val campaignRepository: CampaignRepository,
    private val campaignSegmentsRepository: CampaignSegmentsRepository,
    private val segmentReadPort: SegmentReadPort,
    private val transactionSynchronizationTemplate: TransactionSynchronizationTemplate,
    private val campaignCacheManager: CampaignCacheManager
) {
    @Transactional
    suspend fun execute(input: UpdateCampaignUseCaseIn): UpdateCampaignUseCaseOut {
        val campaign = campaignRepository.findById(input.campaignId)
            ?: throw NotFoundByIdException("Campaign", input.campaignId)

        val normalizedName = input.name.trim()
        ensureNameIsUnique(normalizedName, input.campaignId)

        val requestedSegmentIds = input.segmentIds?.distinct()
        if (requestedSegmentIds != null) {
            ensureSegmentsExist(requestedSegmentIds)
        }

        val previousName = campaign.name
        campaign.name = normalizedName
        campaign.properties = CampaignProperties(
            input.properties.map { CampaignProperty(key = it.key, value = it.value) }
        )

        val updatedCampaign = campaignRepository.save(campaign)

        val resultSegmentIds = if (requestedSegmentIds != null) {
            replaceCampaignSegments(input.campaignId, requestedSegmentIds)
            requestedSegmentIds
        } else {
            campaignSegmentsRepository.findAllByCampaignId(input.campaignId).map { it.segmentId }
        }

        transactionSynchronizationTemplate.afterCommit(Dispatchers.IO, "refresh campaign cache after update") {
            refreshCampaignCache(updatedCampaign.id ?: input.campaignId, previousName, updatedCampaign)
        }

        return UpdateCampaignUseCaseOut(
            id = updatedCampaign.id ?: input.campaignId,
            name = updatedCampaign.name,
            properties = updatedCampaign.properties.value.map {
                CampaignPropertyUseCaseDto(key = it.key, value = it.value)
            },
            segmentIds = resultSegmentIds,
            createdAt = updatedCampaign.createdAt
        )
    }

    private suspend fun ensureNameIsUnique(name: String, campaignId: Long) {
        val duplicated = campaignRepository.findCampaignByName(name)
        if (duplicated != null && duplicated.id != campaignId) {
            throw AlreadyExistsException("Campaign", "name", name)
        }
    }

    private suspend fun ensureSegmentsExist(segmentIds: List<Long>) {
        segmentIds.forEach { segmentId ->
            if (!segmentReadPort.existsById(segmentId)) {
                throw NotFoundByIdException("Segment", segmentId)
            }
        }
    }

    private suspend fun replaceCampaignSegments(campaignId: Long, segmentIds: List<Long>) {
        campaignSegmentsRepository.deleteAllByCampaignId(campaignId)
        segmentIds.forEach { segmentId ->
            campaignSegmentsRepository.save(
                CampaignSegments.new(campaignId = campaignId, segmentId = segmentId)
            )
        }
    }

    private suspend fun refreshCampaignCache(
        campaignId: Long,
        previousName: String,
        updatedCampaign: Campaign
    ) {
        campaignCacheManager.evict(campaignId, previousName)
        campaignCacheManager.save(updatedCampaign)
    }
}
