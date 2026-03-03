package com.manage.crm.journey.application

enum class JourneyTriggerType {
    EVENT,
    SEGMENT,
    CONDITION;

    companion object {
        fun from(value: String): JourneyTriggerType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported triggerType: $value")
        }
    }
}

enum class JourneySegmentTriggerEventType {
    ENTER,
    EXIT,
    UPDATE,
    COUNT_REACHED,
    COUNT_DROPPED;

    companion object {
        fun from(value: String): JourneySegmentTriggerEventType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported segment trigger event type: $value")
        }
    }
}

enum class JourneyStepType {
    ACTION,
    DELAY,
    BRANCH;

    companion object {
        fun from(value: String): JourneyStepType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported stepType: $value")
        }
    }
}

enum class JourneyExecutionStatus {
    RUNNING,
    SUCCESS,
    FAILED
}

enum class JourneyExecutionHistoryStatus {
    RUNNING,
    SUCCESS,
    FAILED,
    RETRYING,
    SKIPPED,
    SKIPPED_DUPLICATE
}

enum class JourneyLifecycleStatus {
    ACTIVE,
    PAUSED,
    ARCHIVED;

    companion object {
        fun from(value: String): JourneyLifecycleStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported lifecycle status: $value")
        }
    }
}

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
    val retryCount: Int
)

data class PostJourneyIn(
    val name: String,
    val triggerType: JourneyTriggerType,
    val triggerEventName: String?,
    val triggerSegmentId: Long?,
    val triggerSegmentEvent: JourneySegmentTriggerEventType?,
    val triggerSegmentWatchFields: List<String>,
    val triggerSegmentCountThreshold: Long?,
    val active: Boolean,
    val steps: List<PostJourneyStepIn>
)

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
    val createdAt: String
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
    val createdAt: String
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
    val updatedAt: String?
)

data class JourneyExecutionHistoryDto(
    val id: Long,
    val journeyExecutionId: Long,
    val journeyStepId: Long,
    val status: String,
    val attempt: Int,
    val message: String?,
    val idempotencyKey: String?,
    val createdAt: String
)
