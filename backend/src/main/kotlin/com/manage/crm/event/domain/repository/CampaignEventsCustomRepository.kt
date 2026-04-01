package com.manage.crm.event.domain.repository

import java.time.LocalDateTime

interface CampaignEventsCustomRepository {
    suspend fun findEventIdsByCampaignId(campaignId: Long): List<Long>

    suspend fun findEventIdsByCampaignIdAndUserId(
        campaignId: Long,
        userId: Long,
    ): List<Long>

    suspend fun findEventIdsByCampaignIdAndCreatedAtRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
    ): List<Long>

    suspend fun countEventsByCampaignIdAndCreatedAtRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
    ): Long

    suspend fun countDistinctUsersByCampaignIdAndCreatedAtRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
    ): Long
}
