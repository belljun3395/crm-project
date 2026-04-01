package com.manage.crm.event.application.dto

import java.time.LocalDateTime

data class ListCampaignsUseCaseIn(
    val limit: Int = 100,
)

data class ListCampaignsUseCaseOut(
    val campaigns: List<CampaignListItemUseCaseDto>,
)

data class CampaignListItemUseCaseDto(
    val id: Long,
    val name: String,
    val createdAt: LocalDateTime?,
)

data class GetCampaignUseCaseIn(
    val campaignId: Long,
)

data class GetCampaignUseCaseOut(
    val id: Long,
    val name: String,
    val properties: List<CampaignPropertyUseCaseDto>,
    val segmentIds: List<Long>,
    val createdAt: LocalDateTime?,
)

data class UpdateCampaignUseCaseIn(
    val campaignId: Long,
    val name: String,
    val properties: List<CampaignPropertyUseCaseDto>,
    val segmentIds: List<Long>? = null,
)

data class UpdateCampaignUseCaseOut(
    val id: Long,
    val name: String,
    val properties: List<CampaignPropertyUseCaseDto>,
    val segmentIds: List<Long>,
    val createdAt: LocalDateTime?,
)

data class DeleteCampaignUseCaseIn(
    val campaignId: Long,
)

data class DeleteCampaignUseCaseOut(
    val success: Boolean,
)

data class CampaignPropertyUseCaseDto(
    val key: String,
    val value: String,
)

data class StreamCampaignDashboardUseCaseIn(
    val campaignId: Long,
    val durationSeconds: Long = 3600,
    val lastEventId: String? = null,
)
