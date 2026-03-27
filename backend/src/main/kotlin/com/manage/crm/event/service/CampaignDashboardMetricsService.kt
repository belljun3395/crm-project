package com.manage.crm.event.service

import com.manage.crm.event.domain.MetricType
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.event.domain.repository.CampaignDashboardMetricsRepository
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.stream.CampaignDashboardEvent
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Coordinates campaign dashboard metric persistence from streamed events.
 */
@Service
class CampaignDashboardMetricsService(
    private val campaignEventsRepository: CampaignEventsRepository,
    private val campaignDashboardMetricsRepository: CampaignDashboardMetricsRepository
) {
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
                        upsertEventCountMetric(campaignId, unit, start, end, windowEvents.size.toLong())

                        val totalUserCount =
                            campaignEventsRepository.countEventsByCampaignIdAndCreatedAtRange(campaignId, start, end)
                        val uniqueUserCount =
                            campaignEventsRepository.countDistinctUsersByCampaignIdAndCreatedAtRange(
                                campaignId,
                                start,
                                end
                            )

                        upsertAbsoluteMetric(campaignId, MetricType.TOTAL_USER_COUNT, unit, start, end, totalUserCount)
                        upsertAbsoluteMetric(
                            campaignId,
                            MetricType.UNIQUE_USER_COUNT,
                            unit,
                            start,
                            end,
                            uniqueUserCount
                        )
                    }
            }
        }
    }

    private suspend fun upsertEventCountMetric(
        campaignId: Long,
        timeWindowUnit: TimeWindowUnit,
        timeWindowStart: LocalDateTime,
        timeWindowEnd: LocalDateTime,
        metricValue: Long
    ) {
        campaignDashboardMetricsRepository.upsertMetric(
            campaignId = campaignId,
            metricType = MetricType.EVENT_COUNT,
            metricValue = metricValue,
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

    private fun calculateTimeWindow(
        timestamp: LocalDateTime,
        unit: TimeWindowUnit
    ): Pair<LocalDateTime, LocalDateTime> {
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
                val start = timestamp.truncatedTo(ChronoUnit.DAYS).with(java.time.DayOfWeek.MONDAY)
                val end = start.plusWeeks(1)
                start to end
            }

            TimeWindowUnit.MONTH -> {
                val start = timestamp.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1)
                val end = start.plusMonths(1)
                start to end
            }
        }
    }
}
