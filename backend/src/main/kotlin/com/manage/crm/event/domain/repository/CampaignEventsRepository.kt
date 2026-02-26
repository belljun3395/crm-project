package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.CampaignEvents
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

/**
 * Repository for querying campaign-linked events and aggregate counts.
 */
interface CampaignEventsRepository : CoroutineCrudRepository<CampaignEvents, Long> {
    suspend fun findAllByCampaignId(campaignId: Long): List<CampaignEvents>

    @Query(
        """
        SELECT ce.* FROM campaign_events ce
        LEFT JOIN events e ON ce.event_id = e.id
        WHERE ce.campaign_id = :campaignId
        AND e.user_id = :userId
        """
    )
    suspend fun findAllByCampaignIdAndUserId(campaignId: Long, userId: Long): List<CampaignEvents>

    @Query(
        """
        SELECT ce.* FROM campaign_events ce
        LEFT JOIN events e ON ce.event_id = e.id
        WHERE ce.campaign_id = :campaignId
        AND e.created_at >= :startTime
        AND e.created_at < :endTime
        """
    )
    suspend fun findAllByCampaignIdAndTimeRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<CampaignEvents>

    @Query(
        """
        SELECT COUNT(*) FROM campaign_events ce
        LEFT JOIN events e ON ce.event_id = e.id
        WHERE ce.campaign_id = :campaignId
          AND e.created_at >= :startTime
          AND e.created_at < :endTime
        """
    )
    suspend fun countAllByCampaignIdAndTimeRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Long

    @Query(
        """
        SELECT COUNT(DISTINCT e.user_id) FROM campaign_events ce
        LEFT JOIN events e ON ce.event_id = e.id
        WHERE ce.campaign_id = :campaignId
          AND e.created_at >= :startTime
          AND e.created_at < :endTime
        """
    )
    suspend fun countDistinctUsersByCampaignIdAndTimeRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Long
}
