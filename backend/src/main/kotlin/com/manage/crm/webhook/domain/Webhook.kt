package com.manage.crm.webhook.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("webhooks")
class Webhook(
    @Id
    var id: Long? = null,
    @Column("name")
    var name: String,
    @Column("url")
    var url: String,
    @Column("events")
    var events: WebhookEvents,
    @Column("active")
    var active: Boolean,
    @CreatedDate
    var createdAt: LocalDateTime? = null
) {
    companion object {
        fun new(name: String, url: String, events: WebhookEvents, active: Boolean): Webhook {
            return Webhook(
                name = name,
                url = url,
                events = events,
                active = active
            )
        }

        fun new(
            id: Long,
            name: String,
            url: String,
            events: WebhookEvents,
            active: Boolean,
            createdAt: LocalDateTime
        ): Webhook {
            return Webhook(
                id = id,
                name = name,
                url = url,
                events = events,
                active = active,
                createdAt = createdAt
            )
        }
    }
}
