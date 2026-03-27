package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.CampaignEvents
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

private const val CAMPAIGN_EVENTS_JOIN_EVENTS = """
    FROM campaign_events ce
    INNER JOIN events e ON ce.event_id = e.id
"""

private const val CAMPAIGN_AND_USER_CONDITION = """
    ce.campaign_id = :campaignId
    AND e.user_id = :userId
"""

private const val CAMPAIGN_AND_CREATED_AT_RANGE_CONDITION = """
    ce.campaign_id = :campaignId
    AND e.created_at >= :startTime
    AND e.created_at < :endTime
"""

/**
 * Repository for querying campaign-linked events and aggregate counts.
 */
interface CampaignEventsRepository : CoroutineCrudRepository<CampaignEvents, Long> {
    @Query(
        """
        SELECT ce.event_id
        FROM campaign_events ce
        WHERE ce.campaign_id = :campaignId
        """
    )
    suspend fun findEventIdsByCampaignId(campaignId: Long): List<Long>

    @Query(
        """
        SELECT ce.event_id
        $CAMPAIGN_EVENTS_JOIN_EVENTS
        WHERE $CAMPAIGN_AND_USER_CONDITION
        """
    )
    suspend fun findEventIdsByCampaignIdAndUserId(campaignId: Long, userId: Long): List<Long>

    @Query(
        """
        SELECT ce.event_id
        $CAMPAIGN_EVENTS_JOIN_EVENTS
        WHERE $CAMPAIGN_AND_CREATED_AT_RANGE_CONDITION
        """
    )
    suspend fun findEventIdsByCampaignIdAndCreatedAtRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<Long>

    @Query(
        """
        SELECT COUNT(*)
        $CAMPAIGN_EVENTS_JOIN_EVENTS
        WHERE $CAMPAIGN_AND_CREATED_AT_RANGE_CONDITION
        """
    )
    suspend fun countEventsByCampaignIdAndCreatedAtRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Long

    @Query(
        """
        SELECT COUNT(DISTINCT e.user_id)
        $CAMPAIGN_EVENTS_JOIN_EVENTS
        WHERE $CAMPAIGN_AND_CREATED_AT_RANGE_CONDITION
        """
    )
    suspend fun countDistinctUsersByCampaignIdAndCreatedAtRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Long
}
