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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// TODO: check concurrency issues: existsCampaignsByName and save
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

        val savedCampaign = campaignRepository.save(
            Campaign.new(
                name = campaignName,
                properties = CampaignProperties(
                    properties.map { (key, value) ->
                        CampaignProperty(key = key, value = value)
                    }
                )
            )
        )

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
}
