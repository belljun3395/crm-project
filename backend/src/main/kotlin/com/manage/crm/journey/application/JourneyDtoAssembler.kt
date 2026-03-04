package com.manage.crm.journey.application

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import java.time.format.DateTimeFormatter

private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

fun assembleJourneyDto(
    journey: Journey,
    steps: List<JourneyStep>,
    objectMapper: ObjectMapper
): JourneyDto {
    val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
    return JourneyDto(
        id = journeyId,
        name = journey.name,
        triggerType = journey.triggerType,
        triggerEventName = journey.triggerEventName,
        triggerSegmentId = journey.triggerSegmentId,
        triggerSegmentEvent = journey.triggerSegmentEvent,
        triggerSegmentWatchFields = fromTriggerSegmentWatchFieldsJson(journey.triggerSegmentWatchFields, objectMapper),
        triggerSegmentCountThreshold = journey.triggerSegmentCountThreshold,
        active = journey.active,
        lifecycleStatus = journey.lifecycleStatus,
        version = journey.version,
        steps = steps.map { step ->
            JourneyStepDto(
                id = requireNotNull(step.id) { "JourneyStep id cannot be null" },
                stepOrder = step.stepOrder,
                stepType = step.stepType,
                channel = step.channel,
                destination = step.destination,
                subject = step.subject,
                body = step.body,
                variables = fromVariablesJson(step.variablesJson, objectMapper),
                delayMillis = step.delayMillis,
                conditionExpression = step.conditionExpression,
                retryCount = step.retryCount,
                createdAt = step.createdAt?.format(formatter) ?: ""
            )
        },
        createdAt = journey.createdAt?.format(formatter) ?: ""
    )
}

fun fromVariablesJson(
    variablesJson: String?,
    objectMapper: ObjectMapper
): Map<String, String> {
    if (variablesJson.isNullOrBlank()) {
        return emptyMap()
    }
    return runCatching {
        objectMapper.readValue(variablesJson, object : TypeReference<Map<String, String>>() {})
    }.getOrElse {
        emptyMap()
    }
}

fun fromTriggerSegmentWatchFieldsJson(
    json: String?,
    objectMapper: ObjectMapper
): List<String> {
    if (json.isNullOrBlank()) {
        return emptyList()
    }
    return runCatching {
        objectMapper.readValue(json, object : TypeReference<List<String>>() {})
    }.getOrElse {
        emptyList()
    }
}
