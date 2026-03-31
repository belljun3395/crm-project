package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.Campaign
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Repository for campaign aggregate lookup and uniqueness checks.
 */
interface CampaignRepository : CoroutineCrudRepository<Campaign, Long>, CampaignCustomRepository {

    suspend fun existsCampaignsByName(name: String): Boolean

    suspend fun findCampaignByName(name: String): Campaign?

    @Query(
        """
        SELECT *
        FROM campaigns
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    override fun findRecentCampaigns(limit: Int): Flow<Campaign>
}
