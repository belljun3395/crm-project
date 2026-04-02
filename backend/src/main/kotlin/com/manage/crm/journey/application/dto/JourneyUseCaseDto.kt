package com.manage.crm.journey.application.dto

import com.manage.crm.event.domain.Event

data class PostJourneyStepIn(
    val stepOrder: Int,
    val stepType: JourneyStepType,
    val channel: String?,
    val destination: String?,
    val subject: String?,
    val body: String?,
    val variables: Map<String, String>,
    val delayMillis: Long?,
    val conditionExpression: String?,
    val retryCount: Int,
)

data class PostJourneyUseCaseIn(
    val name: String,
    val triggerType: JourneyTriggerType,
    val triggerEventName: String?,
    val triggerSegmentId: Long?,
    val triggerSegmentEvent: JourneySegmentTriggerEventType?,
    val triggerSegmentWatchFields: List<String>,
    val triggerSegmentCountThreshold: Long?,
    val active: Boolean,
    val steps: List<PostJourneyStepIn>,
)

data class BrowseJourneyUseCaseIn(
    val limit: Int = 50,
)

data class BrowseJourneyExecutionUseCaseIn(
    val journeyId: Long?,
    val eventId: Long?,
    val userId: Long?,
)

data class BrowseJourneyExecutionHistoryUseCaseIn(
    val journeyExecutionId: Long,
)

data class UpdateJourneyLifecycleStatusUseCaseIn(
    val journeyId: Long,
    val action: JourneyLifecycleAction,
)

data class JourneyAutomationUseCaseIn(
    val event: Event? = null,
    val changedUserIds: List<Long>? = null,
)

enum class JourneyLifecycleAction {
    PAUSE,
    RESUME,
    ARCHIVE,
}

data class PostJourneyUseCaseOut(val journey: JourneyDto)

data class PutJourneyUseCaseOut(val journey: JourneyDto)

data class BrowseJourneyUseCaseOut(val journeys: List<JourneyDto>)

data class BrowseJourneyExecutionUseCaseOut(val executions: List<JourneyExecutionDto>)

data class BrowseJourneyExecutionHistoryUseCaseOut(val histories: List<JourneyExecutionHistoryDto>)

data class UpdateJourneyLifecycleStatusUseCaseOut(val journey: JourneyDto)
