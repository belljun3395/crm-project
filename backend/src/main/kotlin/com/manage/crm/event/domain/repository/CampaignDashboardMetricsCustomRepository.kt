package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.MetricType
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.event.domain.repository.projection.CampaignSummaryMetricsProjection
import java.time.LocalDateTime

interface CampaignDashboardMetricsCustomRepository {
    suspend fun upsertMetric(
        campaignId: Long,
        metricType: MetricType,
        metricValue: Long,
        timeWindowStart: LocalDateTime,
        timeWindowEnd: LocalDateTime,
        timeWindowUnit: TimeWindowUnit,
    ): Int

    suspend fun upsertMetricAbsolute(
        campaignId: Long,
        metricType: MetricType,
        metricValue: Long,
        timeWindowStart: LocalDateTime,
        timeWindowEnd: LocalDateTime,
        timeWindowUnit: TimeWindowUnit,
    ): Int

    suspend fun getCampaignSummaryMetrics(
        campaignId: Long,
        last24Hours: LocalDateTime,
        last7Days: LocalDateTime,
    ): CampaignSummaryMetricsProjection
}
