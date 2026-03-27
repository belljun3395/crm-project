package com.manage.crm.event.domain.repository.projection

data class CampaignSummaryMetricsProjection(
    val totalEvents: Long?,
    val eventsLast24Hours: Long?,
    val eventsLast7Days: Long?
)
