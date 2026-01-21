package com.manage.crm.webhook.domain.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.webhook.domain.Webhook
import com.manage.crm.webhook.domain.WebhookEvents
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@ConditionalOnProperty(name = ["webhook.enabled"], havingValue = "true", matchIfMissing = true)
class WebhookRepositoryCustomImpl(
    private val dataBaseClient: DatabaseClient,
    private val objectMapper: ObjectMapper
) : WebhookRepositoryCustom {
    override suspend fun findActiveByEvent(eventType: String): List<Webhook> {
        val sql = """
            SELECT * FROM webhooks
            WHERE active = true
              AND JSON_CONTAINS(events, :eventJson)
        """.trimIndent()
        val eventJson = objectMapper.writeValueAsString(eventType)

        return dataBaseClient.sql(sql)
            .bind("eventJson", eventJson)
            .fetch()
            .all()
            .map { row ->
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
            .collectList()
            .awaitFirst()
    }
}
