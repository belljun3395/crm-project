package com.manage.crm.email.domain.repository

import com.manage.crm.email.domain.ScheduledEvent
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.jooq.CrmJooqTables
import com.manage.crm.infrastructure.jooq.JooqR2dbcExecutor
import com.manage.crm.infrastructure.jooq.requireLocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL.concat
import org.jooq.impl.DSL.inline
import org.springframework.stereotype.Repository

@Repository
class ScheduledEventCustomRepositoryImpl(
    private val dslContext: DSLContext,
    private val jooqExecutor: JooqR2dbcExecutor,
) : ScheduledEventCustomRepository {
    override suspend fun findAllByEmailTemplateIdAndCompletedFalse(templateId: Long): List<ScheduledEvent> {
        val likePattern = concat(inline("%\"templateId\":"), inline(templateId.toString()), inline("%"))
        val query =
            dslContext
                .select()
                .from(CrmJooqTables.ScheduledEvents.table)
                .where(CrmJooqTables.ScheduledEvents.eventPayload.like(likePattern))
                .and(CrmJooqTables.ScheduledEvents.completed.eq(false))

        return jooqExecutor.fetchList(query, ::toScheduledEvent)
    }

    override suspend fun findByEventIdAndCompletedFalseForUpdate(eventId: EventId): ScheduledEvent? {
        val query =
            dslContext
                .select()
                .from(CrmJooqTables.ScheduledEvents.table)
                .where(CrmJooqTables.ScheduledEvents.eventId.eq(eventId.value))
                .and(CrmJooqTables.ScheduledEvents.completed.eq(false))
                .forUpdate()

        return jooqExecutor.fetchOne(query, ::toScheduledEvent)
    }

    private fun toScheduledEvent(row: Map<String, Any>): ScheduledEvent =
        ScheduledEvent.new(
            id = (row["id"] as Number).toLong(),
            eventId = EventId(row["event_id"] as String),
            eventClass = row["event_class"] as String,
            eventPayload = row["event_payload"] as String,
            completed = toBoolean(row["completed"]),
            isNotConsumed = toBoolean(row["is_not_consumed"]),
            canceled = toBoolean(row["canceled"]),
            scheduledAt = row["scheduled_at"] as String,
            createdAt = row.requireLocalDateTime("created_at"),
        )

    private fun toBoolean(value: Any?): Boolean =
        when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is ByteArray -> value.firstOrNull()?.toInt() != 0
            is String -> value == "1" || value.equals("true", ignoreCase = true)
            else -> false
        }
}
