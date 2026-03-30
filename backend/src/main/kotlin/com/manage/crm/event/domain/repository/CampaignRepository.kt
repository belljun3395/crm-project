package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.Campaign
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Repository for campaign aggregate lookup and uniqueness checks.
 */
interface CampaignRepository : CoroutineCrudRepository<Campaign, Long>, CampaignCustomRepository {

    suspend fun existsCampaignsByName(name: String): Boolean

    suspend fun findCampaignByName(name: String): Campaign?
}
