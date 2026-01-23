package com.manage.crm.event.domain

import com.manage.crm.event.domain.vo.EventProperties
import java.time.LocalDateTime
import kotlin.random.Random

class EventFixtures private constructor() {
    private var id: Long = -1L
    private var name: String = "default-event-name"
    private var userId: Long = -1L
    private var properties: EventProperties = PropertiesFixtures.giveMeOne().buildEvent()
    private var createdAt: LocalDateTime = LocalDateTime.now()

    fun withId(id: Long) = apply { this.id = id }
    fun withName(name: String) = apply { this.name = name }
    fun withUserId(userId: Long) = apply { this.userId = userId }
    fun withProperties(properties: EventProperties) = apply { this.properties = properties }
    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun build(): Event = Event(
        id = id,
        name = name,
        userId = userId,
        properties = properties,
        createdAt = createdAt
    )

    companion object {
        fun anEvent() = EventFixtures()

        fun giveMeOne(): EventFixtures {
            val id = Random.nextLong(1, 101)
            val name = "event_name" + Random.nextLong(1, 101)
            val userId = Random.nextLong(1, 101)
            val properties = PropertiesFixtures.giveMeOne().buildEvent()
            return anEvent()
                .withId(id)
                .withName(name)
                .withUserId(userId)
                .withProperties(properties)
        }
    }
}
