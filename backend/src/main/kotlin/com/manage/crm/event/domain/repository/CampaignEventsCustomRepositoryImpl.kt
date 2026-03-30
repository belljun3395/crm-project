package com.manage.crm.event.domain.repository

import com.manage.crm.infrastructure.jooq.CrmJooqTables
import com.manage.crm.infrastructure.jooq.JooqR2dbcExecutor
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.countDistinct
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CampaignEventsCustomRepositoryImpl(
    private val dslContext: DSLContext,
    private val jooqExecutor: JooqR2dbcExecutor
) : CampaignEventsCustomRepository {

    private val campaignEventsTable = CrmJooqTables.CampaignEvents.table
    private val eventsTable = CrmJooqTables.Events.table
    private val ceCampaignId = CrmJooqTables.CampaignEvents.campaignId
    private val ceEventId = CrmJooqTables.CampaignEvents.eventId
    private val eId = CrmJooqTables.Events.id
    private val eUserId = CrmJooqTables.Events.userId
    private val eCreatedAt = CrmJooqTables.Events.createdAt

    override suspend fun findEventIdsByCampaignId(campaignId: Long): List<Long> {
        val query = dslContext
            .select(ceEventId)
            .from(campaignEventsTable)
            .where(ceCampaignId.eq(campaignId))

        return jooqExecutor.fetchList(query) { (it["event_id"] as Number).toLong() }
    }

    override suspend fun findEventIdsByCampaignIdAndUserId(campaignId: Long, userId: Long): List<Long> {
        val query = dslContext
            .select(ceEventId)
            .from(campaignEventsTable)
            .join(eventsTable)
            .on(ceEventId.eq(eId))
            .where(ceCampaignId.eq(campaignId).and(eUserId.eq(userId)))

        return jooqExecutor.fetchList(query) { (it["event_id"] as Number).toLong() }
    }

    override suspend fun findEventIdsByCampaignIdAndCreatedAtRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<Long> {
        val query = dslContext
            .select(ceEventId)
            .from(campaignEventsTable)
            .join(eventsTable)
            .on(ceEventId.eq(eId))
            .where(
                ceCampaignId.eq(campaignId)
                    .and(eCreatedAt.ge(startTime))
                    .and(eCreatedAt.lt(endTime))
            )

        return jooqExecutor.fetchList(query) { (it["event_id"] as Number).toLong() }
    }

    override suspend fun countEventsByCampaignIdAndCreatedAtRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Long {
        val query = dslContext
            .select(count().`as`("count"))
            .from(campaignEventsTable)
            .join(eventsTable)
            .on(ceEventId.eq(eId))
            .where(
                ceCampaignId.eq(campaignId)
                    .and(eCreatedAt.ge(startTime))
                    .and(eCreatedAt.lt(endTime))
            )

        return jooqExecutor.fetchOne(query) { (it["count"] as Number).toLong() } ?: 0L
    }

    override suspend fun countDistinctUsersByCampaignIdAndCreatedAtRange(
        campaignId: Long,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Long {
        val query = dslContext
            .select(countDistinct(eUserId).`as`("count"))
            .from(campaignEventsTable)
            .join(eventsTable)
            .on(ceEventId.eq(eId))
            .where(
                ceCampaignId.eq(campaignId)
                    .and(eCreatedAt.ge(startTime))
                    .and(eCreatedAt.lt(endTime))
            )

        return jooqExecutor.fetchOne(query) { (it["count"] as Number).toLong() } ?: 0L
    }
}
