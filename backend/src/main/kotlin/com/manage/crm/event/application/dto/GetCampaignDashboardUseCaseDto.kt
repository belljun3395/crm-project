package com.manage.crm.event.application.dto

import com.manage.crm.event.domain.CampaignDashboardMetrics
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.event.service.dto.CampaignDashboardSummary
import java.time.LocalDateTime

data class GetCampaignDashboardUseCaseIn(
    val campaignId: Long,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val timeWindowUnit: TimeWindowUnit? = null
)

data class GetCampaignDashboardUseCaseOut(
    val campaignId: Long,
    val metrics: List<MetricDto>,
    val summary: DashboardSummaryDto
)

data class MetricDto(
    val id: Long?,
    val campaignId: Long,
    val metricType: String,
    val metricValue: Long,
    val timeWindowStart: LocalDateTime,
    val timeWindowEnd: LocalDateTime,
    val timeWindowUnit: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

data class DashboardSummaryDto(
    val campaignId: Long,
    val totalEvents: Long,
    val eventsLast24Hours: Long,
    val eventsLast7Days: Long,
    val lastUpdated: LocalDateTime
)

fun CampaignDashboardMetrics.toDto() = MetricDto(
    id = this.id,
    campaignId = this.campaignId,
    metricType = this.metricType.name,
    metricValue = this.metricValue,
    timeWindowStart = this.timeWindowStart,
    timeWindowEnd = this.timeWindowEnd,
    timeWindowUnit = this.timeWindowUnit.name,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

fun CampaignDashboardSummary.toDto() = DashboardSummaryDto(
    campaignId = this.campaignId,
    totalEvents = this.totalEvents,
    eventsLast24Hours = this.eventsLast24Hours,
    eventsLast7Days = this.eventsLast7Days,
    lastUpdated = this.lastUpdated
)
