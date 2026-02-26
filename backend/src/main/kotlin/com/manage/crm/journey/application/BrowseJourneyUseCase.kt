package com.manage.crm.journey.application

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class BrowseJourneyUseCase(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    suspend fun execute(): List<JourneyDto> {
        val journeys = journeyRepository.findAllByOrderByCreatedAtDesc().toList()

        return journeys.map { journey ->
            val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
            val steps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()

            JourneyDto(
                id = journeyId,
                name = journey.name,
                triggerType = journey.triggerType,
                triggerEventName = journey.triggerEventName,
                triggerSegmentId = journey.triggerSegmentId,
                active = journey.active,
                steps = steps.map { step ->
                    JourneyStepDto(
                        id = requireNotNull(step.id) { "JourneyStep id cannot be null" },
                        stepOrder = step.stepOrder,
                        stepType = step.stepType,
                        channel = step.channel,
                        destination = step.destination,
                        subject = step.subject,
                        body = step.body,
                        variables = fromVariablesJson(step.variablesJson),
                        delayMillis = step.delayMillis,
                        conditionExpression = step.conditionExpression,
                        retryCount = step.retryCount,
                        createdAt = step.createdAt?.format(formatter) ?: ""
                    )
                },
                createdAt = journey.createdAt?.format(formatter) ?: ""
            )
        }
    }

    private fun fromVariablesJson(variablesJson: String?): Map<String, String> {
        if (variablesJson.isNullOrBlank()) {
            return emptyMap()
        }
        return runCatching {
            objectMapper.readValue(variablesJson, object : TypeReference<Map<String, String>>() {})
        }.getOrElse {
            emptyMap()
        }
    }
}
