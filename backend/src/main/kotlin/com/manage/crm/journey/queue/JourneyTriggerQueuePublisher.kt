package com.manage.crm.journey.queue

import com.manage.crm.journey.application.port.out.JourneyTriggerEventPayload
import com.manage.crm.journey.application.port.out.JourneyTriggerPort

interface JourneyTriggerQueuePublisher : JourneyTriggerPort {
    companion object {
        const val TOPIC = "journey-triggers"
    }

    suspend fun publishEventTrigger(event: JourneyEventPayload)

    suspend fun publishSegmentContextTrigger(changedUserIds: List<Long> = emptyList())

    override suspend fun triggerByEvent(event: JourneyTriggerEventPayload) {
        publishEventTrigger(
            JourneyEventPayload(
                id = event.id,
                name = event.name,
                userId = event.userId,
                properties = event.properties.map { JourneyEventPropertyPayload(it.key, it.value) },
                createdAt = event.createdAt,
            ),
        )
    }

    override suspend fun triggerBySegmentContextChange(changedUserIds: List<Long>) {
        publishSegmentContextTrigger(changedUserIds)
    }
}
