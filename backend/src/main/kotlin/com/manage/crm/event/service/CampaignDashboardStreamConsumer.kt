package com.manage.crm.event.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CampaignDashboardStreamConsumer(
    private val campaignStreamRegistry: CampaignStreamRegistry,
    private val streamService: CampaignDashboardStreamService,
    private val campaignDashboardService: CampaignDashboardService
) {
    val log = KotlinLogging.logger { }

    @Scheduled(fixedDelay = 60_000)
    fun processStreamEvents() = runBlocking {
        val activeCampaigns = campaignStreamRegistry.getActiveCampaigns()
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
        val lastProcessedId = campaignStreamRegistry.getLastProcessedId(campaignId)
        val events = streamService.readEventsBatch(campaignId, lastProcessedId)

        if (events.isEmpty()) return

        campaignDashboardService.updateMetricsForEvents(events)

        events.lastOrNull()?.streamId?.let { lastId ->
            campaignStreamRegistry.updateLastProcessedId(campaignId, lastId)
        }

        log.info { "Processed ${events.size} events for campaign: $campaignId" }
    }
}
