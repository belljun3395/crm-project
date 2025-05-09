package com.manage.crm.email.domain.repository

import com.manage.crm.email.domain.ScheduledEvent
import com.manage.crm.email.domain.vo.EventId
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ScheduledEventCustomRepositoryImpl(
    private val dataBaseClient: DatabaseClient
) : ScheduledEventCustomRepository {
    override suspend fun findAllByEmailTemplateIdAndCompletedFalse(templateId: Long): List<ScheduledEvent> {
        var selectQuery = """
            SELECT * FROM SCHEDULED_EVENTS
        """.trimIndent()
        val whereClause = mutableListOf<String>()
        whereClause.add("event_payload LIKE '%\"templateId\":$templateId%'")
        whereClause.add("completed = false")
        selectQuery = selectQuery.plus(" WHERE ${whereClause.joinToString(" AND ")}")

        return dataBaseClient.sql(selectQuery)
            .fetch()
            .all()
            .map {
                ScheduledEvent.new(
                    id = it["id"] as Long,
                    eventId = EventId(it["event_id"] as String),
                    eventClass = it["event_class"] as String,
                    eventPayload = it["event_payload"] as String,
                    completed = it["completed"] as Boolean,
                    isNotConsumed = it["is_not_consumed"] as Boolean,
                    canceled = it["canceled"] as Boolean,
                    scheduledAt = it["scheduled_at"] as String,
                )
            }.collectList().awaitFirst()
    }
}
