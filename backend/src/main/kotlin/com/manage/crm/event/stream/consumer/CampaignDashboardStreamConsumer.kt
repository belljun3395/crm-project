package com.manage.crm.event.stream.consumer

import com.manage.crm.event.service.CampaignDashboardMetricsService
import com.manage.crm.event.stream.CampaignDashboardStreamManager
import com.manage.crm.event.stream.CampaignStreamRegistryManager
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CampaignDashboardStreamConsumer(
    private val campaignStreamRegistryManager: CampaignStreamRegistryManager,
    private val campaignDashboardStreamManager: CampaignDashboardStreamManager,
    private val campaignDashboardMetricsService: CampaignDashboardMetricsService
) {
    companion object {
        private const val MAX_STREAM_LENGTH = 10_000L
    }

    val log = KotlinLogging.logger { }

    @Scheduled(fixedDelay = 60_000)
    fun processStreamEvents() = runBlocking {
        val activeCampaigns = campaignStreamRegistryManager.getActiveCampaigns()
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
        val lastProcessedId = campaignStreamRegistryManager.getLastProcessedId(campaignId)
        val events = campaignDashboardStreamManager.readEventsBatch(campaignId, lastProcessedId)

        if (events.isEmpty()) return

        campaignDashboardMetricsService.updateMetricsForEvents(events)

        events.lastOrNull()?.streamId?.let { lastId ->
            campaignStreamRegistryManager.updateLastProcessedId(campaignId, lastId)
        }

        trimStreamIfNeeded(campaignId)

        log.info { "Processed ${events.size} events for campaign: $campaignId" }
    }

    private suspend fun trimStreamIfNeeded(campaignId: Long) {
        val streamLength = campaignDashboardStreamManager.getStreamLength(campaignId)
        if (streamLength <= MAX_STREAM_LENGTH) {
            return
        }

        campaignDashboardStreamManager.trimStream(campaignId, maxLength = MAX_STREAM_LENGTH)
        log.debug { "Trimmed campaign stream to max length: campaignId=$campaignId, beforeLength=$streamLength" }
    }
}
