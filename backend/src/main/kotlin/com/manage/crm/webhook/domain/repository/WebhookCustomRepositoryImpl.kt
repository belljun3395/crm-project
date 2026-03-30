package com.manage.crm.webhook.domain.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.infrastructure.jooq.CrmJooqTables
import com.manage.crm.infrastructure.jooq.JooqR2dbcExecutor
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEvents
import io.r2dbc.postgresql.codec.Json
import org.jooq.DSLContext
import org.jooq.impl.DSL.condition
import org.jooq.impl.DSL.inline
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
class WebhookCustomRepositoryImpl(
    private val dslContext: DSLContext,
    private val jooqExecutor: JooqR2dbcExecutor,
    private val objectMapper: ObjectMapper
) : WebhookCustomRepository {
    override suspend fun findActiveByEvent(eventType: String): List<Webhook> {
        val query = dslContext
            .select()
            .from(CrmJooqTables.Webhooks.table)
            .where(CrmJooqTables.Webhooks.active.eq(true))
            .and(condition("jsonb_exists({0}, {1})", CrmJooqTables.Webhooks.events, inline(eventType)))

        return jooqExecutor.fetchList(query) { row ->
            val eventsJson = when (val value = row["events"]) {
                is Json -> value.asString()
                else -> value.toString()
            }

            Webhook.new(
                id = (row["id"] as Number).toLong(),
                name = row["name"] as String,
                url = row["url"] as String,
                events = WebhookEvents.fromValues(
                    objectMapper.readValue(eventsJson, List::class.java)
                        .map { it.toString() }
                ),
                active = when (val v = row["active"]) {
                    is Boolean -> v
                    is Number -> v.toInt() != 0
                    else -> throw IllegalStateException("Unexpected active type: ${v?.javaClass}")
                },
                createdAt = row["created_at"] as LocalDateTime
            )
        }
    }
}
