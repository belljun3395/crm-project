package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.CampaignDashboardMetrics
import com.manage.crm.event.domain.MetricType
import com.manage.crm.event.domain.TimeWindowUnit
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface CampaignDashboardMetricsRepository : CoroutineCrudRepository<CampaignDashboardMetrics, Long> {

    suspend fun findByCampaignIdAndMetricTypeAndTimeWindowStartAndTimeWindowEnd(
        campaignId: Long,
        metricType: MetricType,
        timeWindowStart: LocalDateTime,
        timeWindowEnd: LocalDateTime
    ): CampaignDashboardMetrics?

    fun findByCampaignIdAndTimeWindowUnitAndTimeWindowStartAfter(
        campaignId: Long,
        timeWindowUnit: TimeWindowUnit,
        timeWindowStart: LocalDateTime
    ): Flow<CampaignDashboardMetrics>

    fun findByCampaignIdAndTimeWindowStartBetween(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<CampaignDashboardMetrics>

    fun findAllByCampaignIdOrderByTimeWindowStartDesc(
        campaignId: Long
    ): Flow<CampaignDashboardMetrics>

    @Modifying
    @Query(
        """
        UPDATE campaign_dashboard_metrics
        SET metric_value = metric_value + :incrementBy,
            updated_at = CURRENT_TIMESTAMP
        WHERE campaign_id = :campaignId
          AND metric_type = :metricType
          AND time_window_start = :timeWindowStart
          AND time_window_end = :timeWindowEnd
    """
    )
    suspend fun incrementMetricValue(
        campaignId: Long,
        metricType: String,
        timeWindowStart: LocalDateTime,
        timeWindowEnd: LocalDateTime,
        incrementBy: Long
    ): Int

    @Modifying
    @Query(
        """
        INSERT INTO campaign_dashboard_metrics 
            (campaign_id, metric_type, metric_value, time_window_start, time_window_end, time_window_unit, created_at, updated_at)
        VALUES 
            (:campaignId, :metricType, :metricValue, :timeWindowStart, :timeWindowEnd, :timeWindowUnit, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON DUPLICATE KEY UPDATE 
            metric_value = metric_value + VALUES(metric_value),
            updated_at = CURRENT_TIMESTAMP
        """
    )
    suspend fun upsertMetric(
        campaignId: Long,
        metricType: String,
        metricValue: Long,
        timeWindowStart: LocalDateTime,
        timeWindowEnd: LocalDateTime,
        timeWindowUnit: String
    ): Int

    @Query(
        """
        SELECT 
            COALESCE(SUM(metric_value), 0) as total_events,
            COALESCE(SUM(CASE WHEN time_window_start > :last24Hours THEN metric_value ELSE 0 END), 0) as events_last_24_hours,
            COALESCE(SUM(CASE WHEN time_window_start > :last7Days THEN metric_value ELSE 0 END), 0) as events_last_7_days
        FROM campaign_dashboard_metrics
        WHERE campaign_id = :campaignId
          AND metric_type = 'EVENT_COUNT'
          AND time_window_unit = 'HOUR'
        """
    )
    suspend fun getCampaignSummaryMetrics(
        campaignId: Long,
        last24Hours: LocalDateTime,
        last7Days: LocalDateTime
    ): CampaignSummaryMetricsProjection
}

data class CampaignSummaryMetricsProjection(
    val totalEvents: Long,
    val eventsLast24Hours: Long,
    val eventsLast7Days: Long
)
