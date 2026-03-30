package com.manage.crm.email.domain.repository

import com.manage.crm.email.domain.ScheduledEvent
import com.manage.crm.email.domain.vo.EventId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ScheduledEventCustomRepositoryImpl(
    private val dataBaseClient: DatabaseClient
) : ScheduledEventCustomRepository {
    override suspend fun findAllByEmailTemplateIdAndCompletedFalse(templateId: Long): List<ScheduledEvent> {
        val selectQuery = """
            SELECT * FROM scheduled_events
            WHERE event_payload LIKE CONCAT('%\"templateId\":', :templateId, '%')
              AND completed = false
        """.trimIndent()

        return dataBaseClient.sql(selectQuery)
            .bind("templateId", templateId)
            .fetch()
            .all()
            .map(::toScheduledEvent)
            .collectList()
            .awaitFirst()
    }

    override suspend fun findByEventIdAndCompletedFalseForUpdate(eventId: EventId): ScheduledEvent? {
        return dataBaseClient.sql(
            """
            SELECT * FROM scheduled_events
            WHERE event_id = :eventId
              AND completed = false
            FOR UPDATE
            """.trimIndent()
        )
            .bind("eventId", eventId.value)
            .fetch()
            .one()
            .map(::toScheduledEvent)
            .awaitFirstOrNull()
    }

    private fun toScheduledEvent(row: Map<String, Any>): ScheduledEvent {
        return ScheduledEvent.new(
            id = (row["id"] as Number).toLong(),
            eventId = EventId(row["event_id"] as String),
            eventClass = row["event_class"] as String,
            eventPayload = row["event_payload"] as String,
            completed = toBoolean(row["completed"]),
            isNotConsumed = toBoolean(row["is_not_consumed"]),
            canceled = toBoolean(row["canceled"]),
            scheduledAt = row["scheduled_at"] as String,
            createdAt = row["created_at"] as LocalDateTime
        )
    }

    private fun toBoolean(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is ByteArray -> value.firstOrNull()?.toInt() != 0
            is String -> value == "1" || value.equals("true", ignoreCase = true)
            else -> false
        }
    }
}
