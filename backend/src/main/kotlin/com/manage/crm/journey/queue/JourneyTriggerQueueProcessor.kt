package com.manage.crm.journey.queue

import com.manage.crm.journey.application.JourneyAutomationUseCase
import com.manage.crm.journey.application.dto.JourneyAutomationUseCaseIn
import com.manage.crm.journey.application.dto.JourneyTriggerEvent
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JourneyTriggerQueueProcessor(
    private val journeyAutomationUseCase: JourneyAutomationUseCase,
) {
    suspend fun process(message: JourneyTriggerQueueMessage) {
        when (message.triggerType) {
            JourneyTriggerQueueType.EVENT -> {
                val eventPayload = requireNotNull(message.event) { "event payload is required for EVENT trigger message" }
                journeyAutomationUseCase.execute(
                    JourneyAutomationUseCaseIn(
                        event =
                            JourneyTriggerEvent(
                                id = eventPayload.id,
                                name = eventPayload.name,
                                userId = eventPayload.userId,
                                properties = eventPayload.properties.associate { it.key to it.value },
                                createdAt = eventPayload.createdAt ?: LocalDateTime.now(),
                            ),
                    ),
                )
            }

            JourneyTriggerQueueType.SEGMENT_CONTEXT -> {
                journeyAutomationUseCase.execute(JourneyAutomationUseCaseIn(changedUserIds = message.changedUserIds))
            }
        }
    }
}
