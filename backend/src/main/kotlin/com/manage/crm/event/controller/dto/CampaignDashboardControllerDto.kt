package com.manage.crm.event.controller.dto

import java.time.LocalDateTime

data class CampaignEventData(
    val campaignId: Long,
    val eventId: Long,
    val userId: Long,
    val eventName: String,
    val timestamp: LocalDateTime
)

data class CampaignSummaryResponse(
    val campaignId: Long,
    val totalEvents: Long,
    val eventsLast24Hours: Long,
    val eventsLast7Days: Long,
    val lastUpdated: LocalDateTime
)

data class StreamStatusResponse(
    val campaignId: Long,
    val streamLength: Long,
    val checkedAt: LocalDateTime
)
