package com.manage.crm.event.event

import com.manage.crm.event.stream.CampaignDashboardStreamManager
import com.manage.crm.event.stream.CampaignStreamRegistryManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Publishes campaign dashboard events and keeps active campaign registry in sync.
 */
@Service
class CampaignEventPublisher(
    private val campaignDashboardStreamManager: CampaignDashboardStreamManager,
    private val campaignStreamRegistryManager: CampaignStreamRegistryManager
) {
    /**
     * Publishes campaign dashboard event and marks campaign as active for consumer polling.
     */
    @Transactional
    suspend fun publishCampaignEvent(event: CampaignDashboardEvent) {
        // TODO(transaction-consistency): this transaction does not include Redis side effects.
        // Use afterCommit/outbox semantics when this is called from DB transactions.
        campaignDashboardStreamManager.publishEvent(event)
        campaignStreamRegistryManager.registerCampaign(event.campaignId)
    }
}
