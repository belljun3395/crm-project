package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.Campaign
import kotlinx.coroutines.flow.Flow

interface CampaignCustomRepository {
    fun findRecentCampaigns(limit: Int): Flow<Campaign>
}
