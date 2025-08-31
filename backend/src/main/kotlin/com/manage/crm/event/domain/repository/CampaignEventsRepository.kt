package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.CampaignEvents
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CampaignEventsRepository : CoroutineCrudRepository<CampaignEvents, Long> {
    suspend fun findAllByCampaignId(campaignId: Long): List<CampaignEvents>

    @Query(
        """
        SELECT ce.* FROM campaign_events ce
        LEFT JOIN events e ON ce.event_id = e.id
        WHERE ce.campaign_id = :campaignId 
        AND e.user_id = :userId
        """
    )
    suspend fun findAllByCampaignIdAndUserId(campaignId: Long, userId: Long): List<CampaignEvents>
}
