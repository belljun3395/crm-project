package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.CampaignDashboardMetrics
import com.manage.crm.event.domain.TimeWindowUnit
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository for persisted campaign dashboard metrics across configured time windows.
 */
@Repository
interface CampaignDashboardMetricsRepository :
    CoroutineCrudRepository<CampaignDashboardMetrics, Long>,
    CampaignDashboardMetricsCustomRepository {

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
