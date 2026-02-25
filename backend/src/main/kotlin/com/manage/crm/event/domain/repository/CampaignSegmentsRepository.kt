package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.CampaignSegments
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CampaignSegmentsRepository : CoroutineCrudRepository<CampaignSegments, Long> {
    suspend fun findAllByCampaignId(campaignId: Long): List<CampaignSegments>
}
