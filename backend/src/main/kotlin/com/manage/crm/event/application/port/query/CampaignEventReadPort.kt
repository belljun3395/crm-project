package com.manage.crm.event.application.port.query

interface CampaignEventReadPort {
    suspend fun findEventIdsByCampaignId(campaignId: Long): List<Long>
}
