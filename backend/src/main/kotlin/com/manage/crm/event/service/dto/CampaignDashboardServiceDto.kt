package com.manage.crm.event.service.dto

import java.time.LocalDateTime

data class CampaignDashboardSummary(
    val campaignId: Long,
    val totalEvents: Long,
    val eventsLast24Hours: Long,
    val eventsLast7Days: Long,
    val lastUpdated: LocalDateTime
)
