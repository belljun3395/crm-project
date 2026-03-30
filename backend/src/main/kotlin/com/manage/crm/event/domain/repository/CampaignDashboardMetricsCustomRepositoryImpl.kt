package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.MetricType
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.event.domain.repository.projection.CampaignSummaryMetricsProjection
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CampaignDashboardMetricsCustomRepositoryImpl(
    private val dataBaseClient: DatabaseClient
) : CampaignDashboardMetricsCustomRepository {

    override suspend fun upsertMetric(
        campaignId: Long,
        metricType: MetricType,
        metricValue: Long,
        timeWindowStart: LocalDateTime,
        timeWindowEnd: LocalDateTime,
        timeWindowUnit: TimeWindowUnit
    ): Int {
        return dataBaseClient.sql(
            """
            INSERT INTO campaign_dashboard_metrics
                (campaign_id, metric_type, metric_value, time_window_start, time_window_end, time_window_unit, created_at, updated_at)
            VALUES
                (:campaignId, :metricType, :metricValue, :timeWindowStart, :timeWindowEnd, :timeWindowUnit, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                metric_value = metric_value + VALUES(metric_value),
                updated_at = CURRENT_TIMESTAMP
            """.trimIndent()
        )
            .bind("campaignId", campaignId)
            .bind("metricType", metricType.name)
            .bind("metricValue", metricValue)
            .bind("timeWindowStart", timeWindowStart)
            .bind("timeWindowEnd", timeWindowEnd)
            .bind("timeWindowUnit", timeWindowUnit.name)
            .fetch()
            .rowsUpdated()
            .awaitFirst()
            .toInt()
    }

    override suspend fun upsertMetricAbsolute(
        campaignId: Long,
        metricType: MetricType,
        metricValue: Long,
        timeWindowStart: LocalDateTime,
        timeWindowEnd: LocalDateTime,
        timeWindowUnit: TimeWindowUnit
    ): Int {
        return dataBaseClient.sql(
            """
            INSERT INTO campaign_dashboard_metrics
                (campaign_id, metric_type, metric_value, time_window_start, time_window_end, time_window_unit, created_at, updated_at)
            VALUES
                (:campaignId, :metricType, :metricValue, :timeWindowStart, :timeWindowEnd, :timeWindowUnit, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                metric_value = GREATEST(metric_value, VALUES(metric_value)),
                updated_at = CURRENT_TIMESTAMP
            """.trimIndent()
        )
            .bind("campaignId", campaignId)
            .bind("metricType", metricType.name)
            .bind("metricValue", metricValue)
            .bind("timeWindowStart", timeWindowStart)
            .bind("timeWindowEnd", timeWindowEnd)
            .bind("timeWindowUnit", timeWindowUnit.name)
            .fetch()
            .rowsUpdated()
            .awaitFirst()
            .toInt()
    }

    override suspend fun getCampaignSummaryMetrics(
        campaignId: Long,
        last24Hours: LocalDateTime,
        last7Days: LocalDateTime
    ): CampaignSummaryMetricsProjection {
        return dataBaseClient.sql(
            """
            SELECT
                COALESCE(SUM(metric_value), 0) AS total_events,
                COALESCE(SUM(CASE WHEN time_window_start > :last24Hours THEN metric_value ELSE 0 END), 0) AS events_last_24_hours,
                COALESCE(SUM(CASE WHEN time_window_start > :last7Days THEN metric_value ELSE 0 END), 0) AS events_last_7_days
            FROM campaign_dashboard_metrics
            WHERE campaign_id = :campaignId
              AND metric_type = 'EVENT_COUNT'
              AND time_window_unit = 'HOUR'
            """.trimIndent()
        )
            .bind("campaignId", campaignId)
            .bind("last24Hours", last24Hours)
            .bind("last7Days", last7Days)
            .fetch()
            .one()
            .map { row ->
                CampaignSummaryMetricsProjection(
                    totalEvents = (row["total_events"] as Number).toLong(),
                    eventsLast24Hours = (row["events_last_24_hours"] as Number).toLong(),
                    eventsLast7Days = (row["events_last_7_days"] as Number).toLong()
                )
            }
            .awaitFirstOrNull() ?: CampaignSummaryMetricsProjection(0, 0, 0)
    }
}
