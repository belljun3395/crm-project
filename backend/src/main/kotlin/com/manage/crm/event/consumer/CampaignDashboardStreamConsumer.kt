package com.manage.crm.event.consumer

import com.manage.crm.event.service.CampaignDashboardService
import com.manage.crm.event.service.CampaignDashboardStreamService
import com.manage.crm.event.service.CampaignStreamRegistryService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CampaignDashboardStreamConsumer(
    private val campaignStreamRegistryService: CampaignStreamRegistryService,
    private val streamService: CampaignDashboardStreamService,
    private val campaignDashboardService: CampaignDashboardService
) {
    val log = KotlinLogging.logger { }

    @Scheduled(fixedDelay = 60_000)
    fun processStreamEvents() = runBlocking {
        val activeCampaigns = campaignStreamRegistryService.getActiveCampaigns()
        if (activeCampaigns.isEmpty()) return@runBlocking

        activeCampaigns.forEach { campaignId ->
            try {
                processEventsForCampaign(campaignId)
            } catch (e: Exception) {
                log.error(e) { "Failed to process stream events for campaign: $campaignId" }
            }
        }
    }

    private suspend fun processEventsForCampaign(campaignId: Long) {
        val lastProcessedId = campaignStreamRegistryService.getLastProcessedId(campaignId)
        val events = streamService.readEventsBatch(campaignId, lastProcessedId)

        if (events.isEmpty()) return

        campaignDashboardService.updateMetricsForEvents(events)

        events.lastOrNull()?.streamId?.let { lastId ->
            campaignStreamRegistryService.updateLastProcessedId(campaignId, lastId)
        }

        log.info { "Processed ${events.size} events for campaign: $campaignId" }
    }
}
