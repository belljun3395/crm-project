package com.manage.crm.event.domain.repository.projection

/**
 * Projection for campaign event summary aggregates from dashboard metrics.
 */
data class CampaignSummaryMetricsProjection(
    val totalEvents: Long,
    val eventsLast24Hours: Long,
    val eventsLast7Days: Long
)
