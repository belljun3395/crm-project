package com.manage.crm.event.application

import com.manage.crm.event.application.dto.PostCampaignPropertyDto
import com.manage.crm.event.application.dto.PostCampaignUseCaseIn
import com.manage.crm.event.application.dto.PostCampaignUseCaseOut
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.cache.CampaignCacheManager
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import com.manage.crm.support.exception.AlreadyExistsException
import com.manage.crm.support.out
import com.manage.crm.support.transactional.TransactionSynchronizationTemplate
import kotlinx.coroutines.Dispatchers
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * UC-CAMPAIGN-001
 * Creates a campaign with unique name and property definitions.
 *
 * Input: campaign name and property list.
 * Success: persists campaign and returns campaign id/name/properties.
 * Failure: throws AlreadyExistsException when campaign name already exists.
 * Side effects: writes campaign cache after transaction commit.
 */
@Service
class PostCampaignUseCase(
    private val campaignRepository: CampaignRepository,
    private val transactionSynchronizationTemplate: TransactionSynchronizationTemplate,
    private val campaignCacheManager: CampaignCacheManager
) {
    @Transactional
    suspend fun execute(useCaseIn: PostCampaignUseCaseIn): PostCampaignUseCaseOut {
        val campaignName = useCaseIn.name
        val properties = useCaseIn.properties

        if (campaignRepository.existsCampaignsByName(campaignName)) {
            throw AlreadyExistsException("Campaign", "name", campaignName)
        }

        val savedCampaign = try {
            campaignRepository.save(
                Campaign.new(
                    name = campaignName,
                    properties = CampaignProperties(
                        properties.map { (key, value) ->
                            CampaignProperty(key = key, value = value)
                        }
                    )
                )
            )
        } catch (e: DataIntegrityViolationException) {
            if (isCampaignNameDuplicate(e)) {
                throw AlreadyExistsException("Campaign", "name", campaignName)
            }
            throw e
        }

        transactionSynchronizationTemplate.afterCommit(Dispatchers.IO, "save campaign cache") {
            campaignCacheManager.save(savedCampaign)
        }

        return out {
            PostCampaignUseCaseOut(
                id = savedCampaign.id!!,
                name = savedCampaign.name,
                properties = savedCampaign.properties.value.map {
                    PostCampaignPropertyDto(
                        key = it.key,
                        value = it.value
                    )
                }.toList()
            )
        }
    }

    private fun isCampaignNameDuplicate(exception: DataIntegrityViolationException): Boolean {
        var cause: Throwable? = exception
        while (cause != null) {
            val message = cause.message?.lowercase()
            if (message != null && "uk_campaigns_name" in message) {
                return true
            }
            cause = cause.cause
        }
        return false
    }
}
