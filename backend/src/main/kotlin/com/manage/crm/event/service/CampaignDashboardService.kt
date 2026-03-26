package com.manage.crm.event.service

import com.manage.crm.event.domain.CampaignDashboardMetrics
import com.manage.crm.event.domain.MetricType
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.event.domain.repository.CampaignDashboardMetricsRepository
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class CampaignDashboardSummary(
    val campaignId: Long,
    val totalEvents: Long,
    val eventsLast24Hours: Long,
    val eventsLast7Days: Long,
    val lastUpdated: LocalDateTime
)

@Service
class CampaignDashboardService(
    private val campaignDashboardMetricsRepository: CampaignDashboardMetricsRepository,
    private val campaignEventsRepository: CampaignEventsRepository,
    private val streamService: CampaignDashboardStreamService,
    private val campaignStreamRegistryService: CampaignStreamRegistryService
) {
    val log = KotlinLogging.logger { }
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Publishes a campaign event to Redis Stream and updates metrics
     */
    @Transactional
    suspend fun publishCampaignEvent(event: CampaignDashboardEvent) {
        streamService.publishEvent(event)
        campaignStreamRegistryService.registerCampaign(event.campaignId)

        backgroundScope.launch {
            try {
                val streamLength = streamService.getStreamLength(event.campaignId)
                if (streamLength % 100 == 0L && streamLength > 0) {
                    streamService.trimStream(event.campaignId, maxLength = 10000)
                }
            } catch (e: Exception) {
                log.error(e) { "Failed to trim stream for campaign: ${event.campaignId}" }
            }
        }
    }

    private suspend fun updateOrCreateMetric(
        campaignId: Long,
        metricType: MetricType,
        timeWindowUnit: TimeWindowUnit,
        timeWindowStart: LocalDateTime,
        timeWindowEnd: LocalDateTime,
        incrementBy: Long = 1
    ) {
        campaignDashboardMetricsRepository.upsertMetric(
            campaignId = campaignId,
            metricType = metricType,
            metricValue = incrementBy,
            timeWindowStart = timeWindowStart,
            timeWindowEnd = timeWindowEnd,
            timeWindowUnit = timeWindowUnit
        )
    }

    private suspend fun upsertAbsoluteMetric(
        campaignId: Long,
        metricType: MetricType,
        timeWindowUnit: TimeWindowUnit,
        timeWindowStart: LocalDateTime,
        timeWindowEnd: LocalDateTime,
        metricValue: Long
    ) {
        campaignDashboardMetricsRepository.upsertMetricAbsolute(
            campaignId = campaignId,
            metricType = metricType,
            metricValue = metricValue,
            timeWindowStart = timeWindowStart,
            timeWindowEnd = timeWindowEnd,
            timeWindowUnit = timeWindowUnit
        )
    }

    /**
     * Calculates time window boundaries based on the unit
     */
    private fun calculateTimeWindow(timestamp: LocalDateTime, unit: TimeWindowUnit): Pair<LocalDateTime, LocalDateTime> {
        return when (unit) {
            TimeWindowUnit.MINUTE -> {
                val start = timestamp.truncatedTo(ChronoUnit.MINUTES)
                val end = start.plusMinutes(1)
                start to end
            }
            TimeWindowUnit.HOUR -> {
                val start = timestamp.truncatedTo(ChronoUnit.HOURS)
                val end = start.plusHours(1)
                start to end
            }
            TimeWindowUnit.DAY -> {
                val start = timestamp.truncatedTo(ChronoUnit.DAYS)
                val end = start.plusDays(1)
                start to end
            }
            TimeWindowUnit.WEEK -> {
                val start = timestamp.truncatedTo(ChronoUnit.DAYS)
                    .with(java.time.DayOfWeek.MONDAY)
                val end = start.plusWeeks(1)
                start to end
            }
            TimeWindowUnit.MONTH -> {
                val start = timestamp.truncatedTo(ChronoUnit.DAYS)
                    .withDayOfMonth(1)
                val end = start.plusMonths(1)
                start to end
            }
        }
    }

    /**
     * Gets dashboard metrics for a campaign within a time range
     */
    suspend fun getMetricsForCampaign(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<CampaignDashboardMetrics> {
        return campaignDashboardMetricsRepository
            .findByCampaignIdAndTimeWindowStartBetween(campaignId, startTime, endTime)
            .toList()
    }

    /**
     * Gets metrics for a specific time window unit
     */
    suspend fun getMetricsByTimeUnit(
        campaignId: Long,
        timeWindowUnit: TimeWindowUnit,
        from: LocalDateTime
    ): List<CampaignDashboardMetrics> {
        return campaignDashboardMetricsRepository
            .findByCampaignIdAndTimeWindowUnitAndTimeWindowStartAfter(campaignId, timeWindowUnit, from)
            .toList()
    }

    /**
     * Gets all metrics for a campaign, ordered by time
     */
    suspend fun getAllMetricsForCampaign(campaignId: Long): List<CampaignDashboardMetrics> {
        return campaignDashboardMetricsRepository
            .findAllByCampaignIdOrderByTimeWindowStartDesc(campaignId)
            .toList()
    }

    /**
     * Updates dashboard metrics for a batch of events from the stream consumer.
     * Groups events by campaignId and time window, then batch-upserts to MySQL.
     */
    suspend fun updateMetricsForEvents(events: List<CampaignDashboardEvent>) {
        val timeWindowUnits = listOf(
            TimeWindowUnit.MINUTE,
            TimeWindowUnit.HOUR,
            TimeWindowUnit.DAY,
            TimeWindowUnit.WEEK,
            TimeWindowUnit.MONTH
        )

        events.groupBy { it.campaignId }.forEach { (campaignId, campaignEvents) ->
            timeWindowUnits.forEach { unit ->
                campaignEvents.groupBy { event -> calculateTimeWindow(event.timestamp, unit) }
                    .forEach { (window, windowEvents) ->
                        val (start, end) = window
                        updateOrCreateMetric(campaignId, MetricType.EVENT_COUNT, unit, start, end, windowEvents.size.toLong())

                        val totalUserCount = campaignEventsRepository.countAllByCampaignIdAndTimeRange(campaignId, start, end)
                        val uniqueUserCount = campaignEventsRepository.countDistinctUsersByCampaignIdAndTimeRange(campaignId, start, end)

                        upsertAbsoluteMetric(campaignId, MetricType.TOTAL_USER_COUNT, unit, start, end, totalUserCount)
                        upsertAbsoluteMetric(campaignId, MetricType.UNIQUE_USER_COUNT, unit, start, end, uniqueUserCount)
                    }
            }
        }
    }

    /**
     * Gets real-time event stream for a campaign
     */
    fun streamCampaignEvents(campaignId: Long, lastEventId: String? = null) =
        streamService.streamEvents(campaignId, lastEventId = lastEventId)

    /**
     * Gets the current stream length for monitoring
     */
    suspend fun getStreamLength(campaignId: Long): Long = streamService.getStreamLength(campaignId)

    suspend fun getCampaignSummary(campaignId: Long): CampaignDashboardSummary {
        val now = LocalDateTime.now()
        val last24Hours = now.minusHours(24)
        val last7Days = now.minusDays(7)

        val summaryMetrics = campaignDashboardMetricsRepository.getCampaignSummaryMetrics(
            campaignId = campaignId,
            last24Hours = last24Hours,
            last7Days = last7Days
        )

        return CampaignDashboardSummary(
            campaignId = campaignId,
            totalEvents = summaryMetrics.totalEvents ?: 0L,
            eventsLast24Hours = summaryMetrics.eventsLast24Hours ?: 0L,
            eventsLast7Days = summaryMetrics.eventsLast7Days ?: 0L,
            lastUpdated = now
        )
    }
}
