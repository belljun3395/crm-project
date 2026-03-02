package com.manage.crm.journey.queue

import com.manage.crm.event.domain.Event

interface JourneyTriggerQueuePublisher {
    companion object {
        const val TOPIC = "journey-triggers"
    }

    suspend fun publishEventTrigger(event: Event)

    suspend fun publishSegmentContextTrigger(changedUserIds: List<Long>? = null)
}
