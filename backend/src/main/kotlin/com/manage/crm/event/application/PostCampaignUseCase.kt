package com.manage.crm.event.application

import com.manage.crm.event.application.dto.PostCampaignPropertyDto
import com.manage.crm.event.application.dto.PostCampaignUseCaseIn
import com.manage.crm.event.application.dto.PostCampaignUseCaseOut
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import com.manage.crm.support.out
import org.springframework.stereotype.Service

// TODO: check concurrency issues: existsCampaignsByName and save
@Service
class PostCampaignUseCase(
    private val campaignRepository: CampaignRepository
) {
    suspend fun execute(useCaseIn: PostCampaignUseCaseIn): PostCampaignUseCaseOut {
        val campaignName = useCaseIn.name
        val properties = useCaseIn.properties

        if (campaignRepository.existsCampaignsByName(campaignName)) {
            throw IllegalArgumentException("Campaign already exists with name: $campaignName")
        }

        val savedCampaign = campaignRepository.save(
            Campaign(
                name = campaignName,
                properties = Properties(
                    properties.map { (key, value) ->
                        Property(key = key, value = value)
                    }
                )
            )
        )

        return out {
            PostCampaignUseCaseOut(
                id = savedCampaign.id!!,
                name = savedCampaign.name!!,
                properties = savedCampaign.properties!!.value.map {
                    PostCampaignPropertyDto(
                        key = it.key,
                        value = it.value
                    )
                }.toList()
            )
        }
    }
}
