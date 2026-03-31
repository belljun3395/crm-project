package com.manage.crm.event.adapter.query

import com.manage.crm.event.application.port.query.CampaignEventReadPort
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import org.springframework.stereotype.Component

@Component
class CampaignEventReadAdapter(
    private val campaignEventsRepository: CampaignEventsRepository
) : CampaignEventReadPort {
    override suspend fun findEventIdsByCampaignId(campaignId: Long): List<Long> {
        return campaignEventsRepository.findEventIdsByCampaignId(campaignId)
    }
}
