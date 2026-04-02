package com.manage.crm.journey.application.dto

data class PutJourneyStepIn(
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

data class PutJourneyUseCaseIn(
    val journeyId: Long,
    val name: String,
    val triggerType: JourneyTriggerType,
    val triggerEventName: String?,
    val triggerSegmentId: Long?,
    val triggerSegmentEvent: JourneySegmentTriggerEventType?,
    val triggerSegmentWatchFields: List<String>,
    val triggerSegmentCountThreshold: Long?,
    val active: Boolean,
    val steps: List<PutJourneyStepIn>,
)

data class PutJourneyUseCaseOut(
    val journey: JourneyDto,
)
