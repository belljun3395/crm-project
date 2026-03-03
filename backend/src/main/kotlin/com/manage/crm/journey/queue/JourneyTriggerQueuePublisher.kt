package com.manage.crm.journey.queue

interface JourneyTriggerQueuePublisher {
    companion object {
        const val TOPIC = "journey-triggers"
    }

    suspend fun publishEventTrigger(event: JourneyEventPayload)

    suspend fun publishSegmentContextTrigger(changedUserIds: List<Long>? = null)
}
