package com.manage.crm.journey.queue

import java.time.LocalDateTime

enum class JourneyTriggerQueueType {
    EVENT,
    SEGMENT_CONTEXT
}

data class JourneyEventPropertyPayload(
    val key: String,
    val value: String
)

data class JourneyEventPayload(
    val id: Long,
    val name: String,
    val userId: Long,
    val properties: List<JourneyEventPropertyPayload>,
    val createdAt: LocalDateTime?
)

data class JourneyTriggerQueueMessage(
    val triggerType: JourneyTriggerQueueType,
    val event: JourneyEventPayload? = null,
    val changedUserIds: List<Long>? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
