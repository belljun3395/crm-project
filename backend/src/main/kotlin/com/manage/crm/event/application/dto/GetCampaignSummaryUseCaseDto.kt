package com.manage.crm.event.application.dto

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
