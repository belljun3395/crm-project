package com.manage.crm.event.event

import java.time.LocalDateTime
import kotlin.random.Random

class CampaignDashboardEventFixtures private constructor() {
    private var campaignId: Long = -1L
    private var eventId: Long = -1L
    private var userId: Long = -1L
    private var eventName: String = "default-event"
    private var timestamp: LocalDateTime = LocalDateTime.now()
    private var streamId: String? = null

    fun withCampaignId(campaignId: Long) = apply { this.campaignId = campaignId }
    fun withEventId(eventId: Long) = apply { this.eventId = eventId }
    fun withUserId(userId: Long) = apply { this.userId = userId }
    fun withEventName(eventName: String) = apply { this.eventName = eventName }
    fun withTimestamp(timestamp: LocalDateTime) = apply { this.timestamp = timestamp }
    fun withStreamId(streamId: String?) = apply { this.streamId = streamId }

    fun build() = CampaignDashboardEvent(
        campaignId = campaignId,
        eventId = eventId,
        userId = userId,
        eventName = eventName,
        timestamp = timestamp,
        streamId = streamId
    )

    companion object {
        fun aCampaignDashboardEvent() = CampaignDashboardEventFixtures()

        fun giveMeOne(): CampaignDashboardEventFixtures {
            val id = Random.nextLong(1, 101)
            return aCampaignDashboardEvent()
                .withCampaignId(id)
                .withEventId(Random.nextLong(1, 101))
                .withUserId(Random.nextLong(1, 101))
                .withEventName("event_" + Random.nextInt(1, 10))
                .withStreamId("$id-${Random.nextInt(1, 100)}")
        }
    }
}
