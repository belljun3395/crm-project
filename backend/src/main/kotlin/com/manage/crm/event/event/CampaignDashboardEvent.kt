package com.manage.crm.event.event

import java.time.LocalDateTime

data class CampaignDashboardEvent(
    val campaignId: Long,
    val eventId: Long,
    val userId: Long,
    val eventName: String,
    val timestamp: LocalDateTime,
    val streamId: String? = null,
)
