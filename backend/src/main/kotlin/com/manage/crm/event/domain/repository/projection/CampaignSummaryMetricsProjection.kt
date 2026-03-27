package com.manage.crm.event.domain.repository.projection

import org.springframework.data.relational.core.mapping.Column

/**
 * Projection for campaign event summary aggregates from dashboard metrics.
 */
data class CampaignSummaryMetricsProjection(
    @Column("total_events")
    val totalEvents: Long?,
    @Column("events_last_24_hours")
    val eventsLast24Hours: Long?,
    @Column("events_last_7_days")
    val eventsLast7Days: Long?
)
