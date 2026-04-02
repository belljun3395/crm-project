package com.manage.crm.journey.application.dto

import com.manage.crm.journey.exception.InvalidJourneyException

enum class JourneyTriggerType {
    EVENT,
    SEGMENT,
    CONDITION,
    ;

    companion object {
        fun from(value: String): JourneyTriggerType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw InvalidJourneyException("Unsupported triggerType: $value")
    }
}

enum class JourneySegmentTriggerEventType {
    ENTER,
    EXIT,
    UPDATE,
    COUNT_REACHED,
    COUNT_DROPPED,
    ;

    companion object {
        fun from(value: String): JourneySegmentTriggerEventType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw InvalidJourneyException("Unsupported segment trigger event type: $value")
    }
}

enum class JourneyStepType {
    ACTION,
    DELAY,
    BRANCH,
    ;

    companion object {
        fun from(value: String): JourneyStepType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw InvalidJourneyException("Unsupported stepType: $value")
    }
}

enum class JourneyExecutionStatus {
    RUNNING,
    SUCCESS,
    FAILED,
    ;

    companion object {
        fun from(value: String): JourneyExecutionStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw InvalidJourneyException("Unsupported execution status: $value")
    }
}

enum class JourneyExecutionHistoryStatus {
    RUNNING,
    SUCCESS,
    FAILED,
    RETRYING,
    SKIPPED,
    SKIPPED_DUPLICATE,
    ;

    companion object {
        fun from(value: String): JourneyExecutionHistoryStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw InvalidJourneyException("Unsupported execution history status: $value")
    }
}

enum class JourneyLifecycleStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    ARCHIVED,
    ;

    companion object {
        fun from(value: String): JourneyLifecycleStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw InvalidJourneyException("Unsupported lifecycle status: $value")
    }
}

data class JourneyStepDto(
    val id: Long,
    val stepOrder: Int,
    val stepType: String,
    val channel: String?,
    val destination: String?,
    val subject: String?,
    val body: String?,
    val variables: Map<String, String>,
    val delayMillis: Long?,
    val conditionExpression: String?,
    val retryCount: Int,
    val createdAt: String?,
)

data class JourneyDto(
    val id: Long,
    val name: String,
    val triggerType: String,
    val triggerEventName: String?,
    val triggerSegmentId: Long?,
    val triggerSegmentEvent: String?,
    val triggerSegmentWatchFields: List<String>,
    val triggerSegmentCountThreshold: Long?,
    val active: Boolean,
    val lifecycleStatus: String,
    val version: Int,
    val steps: List<JourneyStepDto>,
    val createdAt: String?,
)

data class JourneyExecutionDto(
    val id: Long,
    val journeyId: Long,
    val eventId: Long,
    val userId: Long,
    val status: String,
    val currentStepOrder: Int,
    val lastError: String?,
    val triggerKey: String,
    val startedAt: String,
    val completedAt: String?,
    val createdAt: String,
    val updatedAt: String?,
)

data class JourneyExecutionHistoryDto(
    val id: Long,
    val journeyExecutionId: Long,
    val journeyStepId: Long,
    val status: String,
    val attempt: Int,
    val message: String?,
    val idempotencyKey: String?,
    val createdAt: String?,
)
