package com.manage.crm.event.domain.repository

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
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

@Repository
class CampaignEventsCustomRepositoryImpl(
    private val dataBaseClient: DatabaseClient
) : CampaignEventsCustomRepository {

    override suspend fun findEventIdsByCampaignId(campaignId: Long): List<Long> {
        return dataBaseClient.sql(
            """
            SELECT ce.event_id
            FROM campaign_events ce
            WHERE ce.campaign_id = :campaignId
            """.trimIndent()
        )
            .bind("campaignId", campaignId)
            .fetch()
            .all()
            .map { (it["event_id"] as Number).toLong() }
            .collectList()
            .awaitFirst()
    }

    override suspend fun findEventIdsByCampaignIdAndUserId(campaignId: Long, userId: Long): List<Long> {
        return dataBaseClient.sql(
            """
            SELECT ce.event_id
            $CAMPAIGN_EVENTS_JOIN_EVENTS
            WHERE $CAMPAIGN_AND_USER_CONDITION
            """.trimIndent()
        )
            .bind("campaignId", campaignId)
            .bind("userId", userId)
            .fetch()
            .all()
            .map { (it["event_id"] as Number).toLong() }
            .collectList()
            .awaitFirst()
    }

    override suspend fun findEventIdsByCampaignIdAndCreatedAtRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<Long> {
        return dataBaseClient.sql(
            """
            SELECT ce.event_id
            $CAMPAIGN_EVENTS_JOIN_EVENTS
            WHERE $CAMPAIGN_AND_CREATED_AT_RANGE_CONDITION
            """.trimIndent()
        )
            .bind("campaignId", campaignId)
            .bind("startTime", startTime)
            .bind("endTime", endTime)
            .fetch()
            .all()
            .map { (it["event_id"] as Number).toLong() }
            .collectList()
            .awaitFirst()
    }

    override suspend fun countEventsByCampaignIdAndCreatedAtRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Long {
        return dataBaseClient.sql(
            """
            SELECT COUNT(*) AS count
            $CAMPAIGN_EVENTS_JOIN_EVENTS
            WHERE $CAMPAIGN_AND_CREATED_AT_RANGE_CONDITION
            """.trimIndent()
        )
            .bind("campaignId", campaignId)
            .bind("startTime", startTime)
            .bind("endTime", endTime)
            .fetch()
            .one()
            .map { (it["count"] as Number).toLong() }
            .awaitFirstOrNull() ?: 0L
    }

    override suspend fun countDistinctUsersByCampaignIdAndCreatedAtRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Long {
        return dataBaseClient.sql(
            """
            SELECT COUNT(DISTINCT e.user_id) AS count
            $CAMPAIGN_EVENTS_JOIN_EVENTS
            WHERE $CAMPAIGN_AND_CREATED_AT_RANGE_CONDITION
            """.trimIndent()
        )
            .bind("campaignId", campaignId)
            .bind("startTime", startTime)
            .bind("endTime", endTime)
            .fetch()
            .one()
            .map { (it["count"] as Number).toLong() }
            .awaitFirstOrNull() ?: 0L
    }
}
