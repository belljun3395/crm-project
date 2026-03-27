package com.manage.crm.event.application.dto

import com.manage.crm.event.domain.repository.projection.CampaignSummaryMetricsProjection
import java.time.LocalDateTime

data class GetCampaignSummaryUseCaseIn(
    val campaignId: Long
)

data class GetCampaignSummaryUseCaseOut(
    val campaignId: Long,
    val totalEvents: Long,
    val eventsLast24Hours: Long,
    val eventsLast7Days: Long,
    val lastUpdated: LocalDateTime
)

fun CampaignSummaryMetricsProjection.toSummaryUseCaseOut(
    campaignId: Long,
    lastUpdated: LocalDateTime
) = GetCampaignSummaryUseCaseOut(
    campaignId = campaignId,
    totalEvents = this.totalEvents,
    eventsLast24Hours = this.eventsLast24Hours,
    eventsLast7Days = this.eventsLast7Days,
    lastUpdated = lastUpdated
)
