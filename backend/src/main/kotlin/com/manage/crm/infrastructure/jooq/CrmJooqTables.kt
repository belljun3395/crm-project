package com.manage.crm.infrastructure.jooq

import org.jooq.Field
import org.jooq.Table
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.table
import java.time.LocalDateTime

object CrmJooqTables {
    object Campaigns {
        val table: Table<*> = table(name("campaigns"))
        val id: Field<Long> = field(name("id"), Long::class.java)
        val name: Field<String> = field(name("name"), String::class.java)
        val properties: Field<String> = field(name("properties"), String::class.java)
        val createdAt: Field<LocalDateTime> = field(name("created_at"), LocalDateTime::class.java)
    }

    object CampaignEvents {
        val table: Table<*> = table(name("campaign_events"))
        val campaignId: Field<Long> = field(name("campaign_id"), Long::class.java)
        val eventId: Field<Long> = field(name("event_id"), Long::class.java)
    }

    object CampaignDashboardMetrics {
        val table: Table<*> = table(name("campaign_dashboard_metrics"))
        val campaignId: Field<Long> = field(name("campaign_id"), Long::class.java)
        val metricType: Field<String> = field(name("metric_type"), String::class.java)
        val metricValue: Field<Long> = field(name("metric_value"), Long::class.java)
        val timeWindowStart: Field<LocalDateTime> = field(name("time_window_start"), LocalDateTime::class.java)
        val timeWindowEnd: Field<LocalDateTime> = field(name("time_window_end"), LocalDateTime::class.java)
        val timeWindowUnit: Field<String> = field(name("time_window_unit"), String::class.java)
        val createdAt: Field<LocalDateTime> = field(name("created_at"), LocalDateTime::class.java)
        val updatedAt: Field<LocalDateTime> = field(name("updated_at"), LocalDateTime::class.java)
    }

    object Events {
        val table: Table<*> = table(name("events"))
        val id: Field<Long> = field(name("id"), Long::class.java)
        val name: Field<String> = field(name("name"), String::class.java)
        val userId: Field<Long> = field(name("user_id"), Long::class.java)
        val properties: Field<String> = field(name("properties"), String::class.java)
        val createdAt: Field<LocalDateTime> = field(name("created_at"), LocalDateTime::class.java)
    }

    object ScheduledEvents {
        val table: Table<*> = table(name("scheduled_events"))
        val id: Field<Long> = field(name("id"), Long::class.java)
        val eventId: Field<String> = field(name("event_id"), String::class.java)
        val eventClass: Field<String> = field(name("event_class"), String::class.java)
        val eventPayload: Field<String> = field(name("event_payload"), String::class.java)
        val completed: Field<Boolean> = field(name("completed"), Boolean::class.java)
        val isNotConsumed: Field<Boolean> = field(name("is_not_consumed"), Boolean::class.java)
        val canceled: Field<Boolean> = field(name("canceled"), Boolean::class.java)
        val scheduledAt: Field<String> = field(name("scheduled_at"), String::class.java)
        val createdAt: Field<LocalDateTime> = field(name("created_at"), LocalDateTime::class.java)
    }

    object Journeys {
        val table: Table<*> = table(name("journeys"))
        val id: Field<Long> = field(name("id"), Long::class.java)
        val lifecycleStatus: Field<String> = field(name("lifecycle_status"), String::class.java)
        val active: Field<Boolean> = field(name("active"), Boolean::class.java)
        val version: Field<Int> = field(name("version"), Int::class.java)
    }

    object Users {
        val table: Table<*> = table(name("users"))
        val id: Field<Long> = field(name("id"), Long::class.java)
        val externalId: Field<String> = field(name("external_id"), String::class.java)
        val userAttributes: Field<String> = field(name("user_attributes"), String::class.java)
        val createdAt: Field<LocalDateTime> = field(name("created_at"), LocalDateTime::class.java)
        val updatedAt: Field<LocalDateTime> = field(name("updated_at"), LocalDateTime::class.java)
    }

    object Webhooks {
        val table: Table<*> = table(name("webhooks"))
        val id: Field<Long> = field(name("id"), Long::class.java)
        val name: Field<String> = field(name("name"), String::class.java)
        val url: Field<String> = field(name("url"), String::class.java)
        val events: Field<String> = field(name("events"), String::class.java)
        val active: Field<Boolean> = field(name("active"), Boolean::class.java)
        val createdAt: Field<LocalDateTime> = field(name("created_at"), LocalDateTime::class.java)
    }
}
