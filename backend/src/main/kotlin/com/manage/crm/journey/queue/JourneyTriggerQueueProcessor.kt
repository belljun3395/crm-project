package com.manage.crm.journey.queue

import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import com.manage.crm.journey.application.JourneyAutomationService
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JourneyTriggerQueueProcessor(
    private val journeyAutomationService: JourneyAutomationService
) {
    suspend fun process(message: JourneyTriggerQueueMessage) {
        when (message.triggerType) {
            JourneyTriggerQueueType.EVENT -> {
                val eventPayload = requireNotNull(message.event) { "event payload is required for EVENT trigger message" }
                journeyAutomationService.onEvent(
                    Event.new(
                        id = eventPayload.id,
                        name = eventPayload.name,
                        userId = eventPayload.userId,
                        properties = EventProperties(
                            eventPayload.properties.map { property ->
                                EventProperty(
                                    key = property.key,
                                    value = property.value
                                )
                            }
                        ),
                        createdAt = eventPayload.createdAt ?: LocalDateTime.now()
                    )
                )
            }

            JourneyTriggerQueueType.SEGMENT_CONTEXT -> {
                journeyAutomationService.onSegmentContextChanged(message.changedUserIds)
            }
        }
    }
}
