package com.manage.crm.email.domain

import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.email.domain.vo.EventIdFixtures
import com.manage.crm.email.domain.vo.ScheduleType
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

class ScheduledEventFixtures private constructor() {
    private var id: Long? = null
    private lateinit var eventId: EventId
    private lateinit var eventClass: String
    private lateinit var eventPayload: String
    private var completed: Boolean = false
    private var isNotConsumed: Boolean = false
    private var canceled: Boolean = false
    private lateinit var scheduledAt: String
    private var createdAt: LocalDateTime? = null

    fun withId(id: Long?) = apply { this.id = id }
    fun withEventId(eventId: EventId) = apply { this.eventId = eventId }
    fun withEventClass(eventClass: String) = apply { this.eventClass = eventClass }
    fun withEventPayload(eventPayload: String) = apply { this.eventPayload = eventPayload }
    fun withCompleted(completed: Boolean) = apply { this.completed = completed }
    fun withIsNotConsumed(isNotConsumed: Boolean) = apply { this.isNotConsumed = isNotConsumed }
    fun withCanceled(canceled: Boolean) = apply { this.canceled = canceled }
    fun withScheduledAt(scheduledAt: String) = apply { this.scheduledAt = scheduledAt }
    fun withCreatedAt(createdAt: LocalDateTime?) = apply { this.createdAt = createdAt }

    fun build(): ScheduledEvent = ScheduledEvent(
        id = id,
        eventId = eventId,
        eventClass = eventClass,
        eventPayload = eventPayload,
        completed = completed,
        isNotConsumed = isNotConsumed,
        canceled = canceled,
        scheduledAt = scheduledAt,
        createdAt = createdAt
    )

    companion object {
        fun aScheduledEvent() = ScheduledEventFixtures()
            .withCreatedAt(LocalDateTime.now())
            .withEventId(EventIdFixtures.anEventId().build())

        fun giveMeOne(): ScheduledEventFixtures {
            val randomSuffix = UUID.randomUUID().toString().substring(0, 8)
            val id = Random.nextLong(1, 101)
            val eventId = EventIdFixtures.giveMeOne().build()
            val eventClass = "com.example.Event$randomSuffix"
            val eventPayload = """{"data":"$randomSuffix"}"""
            val scheduledAt = ScheduleType.APP.name
            return aScheduledEvent()
                .withId(id)
                .withEventId(eventId)
                .withEventClass(eventClass)
                .withEventPayload(eventPayload)
                .withScheduledAt(scheduledAt)
        }
    }
}
