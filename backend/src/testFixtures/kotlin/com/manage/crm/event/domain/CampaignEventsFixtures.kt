package com.manage.crm.event.domain

import java.time.LocalDateTime
import kotlin.random.Random

class CampaignEventsFixtures private constructor() {
    private var id: Long? = null
    private var campaignId: Long = -1L
    private var eventId: Long = -1L
    private lateinit var createdAt: LocalDateTime

    fun withId(id: Long?) = apply { this.id = id }
    fun withCampaignId(campaignId: Long) = apply { this.campaignId = campaignId }
    fun withEventId(eventId: Long) = apply { this.eventId = eventId }
    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }

    fun build() = CampaignEvents(
        id = id,
        campaignId = campaignId,
        eventId = eventId,
        createdAt = createdAt
    )

    companion object {
        fun aCampaignEvents() = CampaignEventsFixtures()
            .withCreatedAt(LocalDateTime.now())

        fun giveMeOne(): CampaignEventsFixtures {
            val id = Random.nextLong(1, 101)
            val campaignId = Random.nextLong(1, 101)
            val eventId = Random.nextLong(1, 101)

            return aCampaignEvents()
                .withId(id)
                .withCampaignId(campaignId)
                .withEventId(eventId)
        }
    }
}
