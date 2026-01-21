package com.manage.crm.event.service

import com.manage.crm.event.domain.CampaignDashboardMetrics
import com.manage.crm.event.domain.MetricType
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.event.domain.repository.CampaignDashboardMetricsRepository
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.service.dto.CampaignDashboardSummary
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

@Service
class CampaignDashboardService(
    private val campaignDashboardMetricsRepository: CampaignDashboardMetricsRepository,
    private val campaignEventsRepository: CampaignEventsRepository,
    private val streamService: CampaignDashboardStreamService
) {
    val log = KotlinLogging.logger { }
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Publishes a campaign event to Redis Stream and updates metrics
     */
    @Transactional
    suspend fun publishCampaignEvent(event: CampaignDashboardEvent) {
        streamService.publishEvent(event)
        updateMetricsForEvent(event)

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

    /**
     * Updates dashboard metrics when a new event occurs
     */
    private suspend fun updateMetricsForEvent(event: CampaignDashboardEvent) {
        val timeWindows = listOf(
            TimeWindowUnit.HOUR to calculateTimeWindow(event.timestamp, TimeWindowUnit.HOUR),
            TimeWindowUnit.DAY to calculateTimeWindow(event.timestamp, TimeWindowUnit.DAY)
        )

        timeWindows.forEach { (unit, window) ->
            val start = window.first
            val end = window.second
            updateOrCreateMetric(
                campaignId = event.campaignId,
                metricType = MetricType.EVENT_COUNT,
                timeWindowUnit = unit,
                timeWindowStart = start,
                timeWindowEnd = end,
                incrementBy = 1
            )
        }

        log.debug { "Updated metrics for campaign event: campaignId=${event.campaignId}, eventId=${event.eventId}" }
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
     * Aggregates metrics from database for a specific time period
     * Useful for backfilling or recalculation
     */
    @Transactional
    suspend fun aggregateMetricsForPeriod(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        timeWindowUnit: TimeWindowUnit
    ): CampaignDashboardMetrics {
        // Get events for the campaign in the specified time period from campaign_events table
        val events = campaignEventsRepository.findAllByCampaignIdAndTimeRange(
            campaignId = campaignId,
            startTime = startTime,
            endTime = endTime
        )

        val window = calculateTimeWindow(startTime, timeWindowUnit)
        val eventCount = events.size.toLong()

        val metric = CampaignDashboardMetrics.new(
            campaignId = campaignId,
            metricType = MetricType.EVENT_COUNT,
            metricValue = eventCount,
            timeWindowStart = window.first,
            timeWindowEnd = window.second,
            timeWindowUnit = timeWindowUnit
        )

        return campaignDashboardMetricsRepository.save(metric)
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
            totalEvents = summaryMetrics.totalEvents,
            eventsLast24Hours = summaryMetrics.eventsLast24Hours,
            eventsLast7Days = summaryMetrics.eventsLast7Days,
            lastUpdated = now
        )
    }
}
