package com.manage.crm.journey.application.dto

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.exception.InvalidJourneyException
import java.time.format.DateTimeFormatter

private val JOURNEY_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

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

fun Journey.toJourneyDto(
    steps: List<JourneyStepDto>,
    objectMapper: ObjectMapper,
): JourneyDto {
    val journeyId = requireNotNull(this.id) { "Journey id cannot be null" }
    return JourneyDto(
        id = journeyId,
        name = this.name,
        triggerType = this.triggerType,
        triggerEventName = this.triggerEventName,
        triggerSegmentId = this.triggerSegmentId,
        triggerSegmentEvent = this.triggerSegmentEvent,
        triggerSegmentWatchFields = this.triggerSegmentWatchFields.toWatchFieldList(objectMapper),
        triggerSegmentCountThreshold = this.triggerSegmentCountThreshold,
        active = this.active,
        lifecycleStatus = this.lifecycleStatus,
        version = this.version,
        steps = steps,
        createdAt = this.createdAt?.format(JOURNEY_DATE_TIME_FORMATTER),
    )
}

fun JourneyStep.toJourneyStepDto(objectMapper: ObjectMapper): JourneyStepDto =
    JourneyStepDto(
        id = requireNotNull(this.id) { "JourneyStep id cannot be null" },
        stepOrder = this.stepOrder,
        stepType = this.stepType,
        channel = this.channel,
        destination = this.destination,
        subject = this.subject,
        body = this.body,
        variables = this.variablesJson.toVariablesMap(objectMapper),
        delayMillis = this.delayMillis,
        conditionExpression = this.conditionExpression,
        retryCount = this.retryCount,
        createdAt = this.createdAt?.format(JOURNEY_DATE_TIME_FORMATTER),
    )

private fun String?.toVariablesMap(objectMapper: ObjectMapper): Map<String, String> {
    if (this.isNullOrBlank()) return emptyMap()
    return runCatching {
        objectMapper.readValue(this, object : TypeReference<Map<String, String>>() {})
    }.getOrElse { emptyMap() }
}

private fun String?.toWatchFieldList(objectMapper: ObjectMapper): List<String> {
    if (this.isNullOrBlank()) return emptyList()
    return runCatching {
        objectMapper.readValue(this, object : TypeReference<List<String>>() {})
    }.getOrElse { emptyList() }
}
