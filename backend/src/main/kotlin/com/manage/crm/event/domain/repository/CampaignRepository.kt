package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.Campaign
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CampaignRepository : CoroutineCrudRepository<Campaign, Long> {

    suspend fun existsCampaignsByName(name: String): Boolean

    suspend fun findCampaignByName(name: String): Campaign?
}
