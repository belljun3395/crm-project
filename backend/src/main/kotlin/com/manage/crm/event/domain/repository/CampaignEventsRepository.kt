package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.CampaignEvents
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CampaignEventsRepository : CoroutineCrudRepository<CampaignEvents, Long> {
    suspend fun findAllByCampaignId(campaignId: Long): List<CampaignEvents>
    suspend fun findTopByEventId(eventId: Long): CampaignEvents?
}
