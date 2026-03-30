package com.manage.crm.webhook.domain.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.infrastructure.jooq.CrmJooqTables
import com.manage.crm.infrastructure.jooq.JooqR2dbcExecutor
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEvents
import org.jooq.DSLContext
import org.jooq.impl.DSL.condition
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
        val eventJson = objectMapper.writeValueAsString(eventType)
        val query = dslContext
            .select()
            .from(CrmJooqTables.Webhooks.table)
            .where(CrmJooqTables.Webhooks.active.eq(true))
            .and(condition("JSON_CONTAINS({0}, {1})", CrmJooqTables.Webhooks.events, eventJson))

        return jooqExecutor.fetchList(query) { row ->
            Webhook.new(
                id = (row["id"] as Number).toLong(),
                name = row["name"] as String,
                url = row["url"] as String,
                events = WebhookEvents.fromValues(
                    objectMapper.readValue(row["events"] as String, List::class.java)
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
