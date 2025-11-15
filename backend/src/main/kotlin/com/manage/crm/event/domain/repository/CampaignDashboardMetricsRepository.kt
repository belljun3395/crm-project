package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.CampaignDashboardMetrics
import com.manage.crm.event.domain.MetricType
import com.manage.crm.event.domain.TimeWindowUnit
import kotlinx.coroutines.flow.Flow
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
}
