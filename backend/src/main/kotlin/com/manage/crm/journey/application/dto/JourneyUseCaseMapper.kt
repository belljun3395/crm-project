package com.manage.crm.journey.application.dto

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import java.time.format.DateTimeFormatter

private val JOURNEY_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

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
