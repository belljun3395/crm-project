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
}
