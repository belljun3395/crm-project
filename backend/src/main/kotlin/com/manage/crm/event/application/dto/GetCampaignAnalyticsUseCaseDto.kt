package com.manage.crm.event.application.dto

import java.time.LocalDateTime

data class GetCampaignFunnelAnalyticsUseCaseIn(
    val campaignId: Long,
    val steps: List<String>,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
)

data class GetCampaignFunnelAnalyticsUseCaseOut(
    val campaignId: Long,
    val stepMetrics: List<FunnelStepMetricDto>,
)

data class FunnelStepMetricDto(
    val step: String,
    val eventCount: Int,
    val qualifiedUserCount: Int,
    val conversionFromPrevious: Double,
)

data class GetCampaignSegmentComparisonUseCaseIn(
    val campaignId: Long,
    val segmentIds: List<Long>,
    val eventName: String?,
    val startTime: LocalDateTime?,
    val endTime: LocalDateTime?,
)

data class GetCampaignSegmentComparisonUseCaseOut(
    val campaignId: Long,
    val eventName: String?,
    val segmentMetrics: List<SegmentComparisonMetricDto>,
)

data class SegmentComparisonMetricDto(
    val segmentId: Long,
    val segmentName: String?,
    val targetUserCount: Int,
    val eventUserCount: Int,
    val eventCount: Int,
    val conversionRate: Double,
)
