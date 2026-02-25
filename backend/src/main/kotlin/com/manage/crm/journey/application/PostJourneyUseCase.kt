package com.manage.crm.journey.application

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class PostJourneyUseCase(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    suspend fun execute(useCaseIn: PostJourneyIn): JourneyDto {
        validate(useCaseIn)

        val savedJourney = journeyRepository.save(
            Journey.new(
                name = useCaseIn.name,
                triggerType = useCaseIn.triggerType.name,
                triggerEventName = useCaseIn.triggerEventName,
                triggerSegmentId = useCaseIn.triggerSegmentId,
                active = useCaseIn.active
            )
        )

        val journeyId = requireNotNull(savedJourney.id) { "Journey id cannot be null" }
        val savedSteps = useCaseIn.steps
            .sortedBy { it.stepOrder }
            .map { step ->
                journeyStepRepository.save(
                    JourneyStep.new(
                        journeyId = journeyId,
                        stepOrder = step.stepOrder,
                        stepType = step.stepType.name,
                        channel = step.channel,
                        destination = step.destination,
                        subject = step.subject,
                        body = step.body,
                        variablesJson = toVariablesJson(step.variables),
                        delayMillis = step.delayMillis,
                        conditionExpression = step.conditionExpression,
                        retryCount = step.retryCount.coerceAtLeast(0)
                    )
                )
            }

        return JourneyDto(
            id = journeyId,
            name = savedJourney.name,
            triggerType = savedJourney.triggerType,
            triggerEventName = savedJourney.triggerEventName,
            triggerSegmentId = savedJourney.triggerSegmentId,
            active = savedJourney.active,
            steps = savedSteps.map { step ->
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
            createdAt = savedJourney.createdAt?.format(formatter) ?: ""
        )
    }

    private fun validate(useCaseIn: PostJourneyIn) {
        if (useCaseIn.name.isBlank()) {
            throw IllegalArgumentException("Journey name is required")
        }

        if (useCaseIn.steps.isEmpty()) {
            throw IllegalArgumentException("Journey steps are required")
        }

        when (useCaseIn.triggerType) {
            JourneyTriggerType.EVENT -> {
                if (useCaseIn.triggerEventName.isNullOrBlank()) {
                    throw IllegalArgumentException("triggerEventName is required for EVENT trigger")
                }
            }

            JourneyTriggerType.SEGMENT -> {
                if (useCaseIn.triggerSegmentId == null) {
                    throw IllegalArgumentException("triggerSegmentId is required for SEGMENT trigger")
                }
            }

            JourneyTriggerType.CONDITION -> Unit
        }

        useCaseIn.steps.forEach { step ->
            when (step.stepType) {
                JourneyStepType.ACTION -> {
                    if (step.channel.isNullOrBlank()) {
                        throw IllegalArgumentException("channel is required for ACTION step")
                    }
                    if (step.destination.isNullOrBlank()) {
                        throw IllegalArgumentException("destination is required for ACTION step")
                    }
                    if (step.body.isNullOrBlank()) {
                        throw IllegalArgumentException("body is required for ACTION step")
                    }
                }

                JourneyStepType.DELAY -> {
                    if (step.delayMillis == null || step.delayMillis < 0) {
                        throw IllegalArgumentException("delayMillis must be zero or greater for DELAY step")
                    }
                }

                JourneyStepType.BRANCH -> {
                    if (step.conditionExpression.isNullOrBlank()) {
                        throw IllegalArgumentException("conditionExpression is required for BRANCH step")
                    }
                }
            }
        }
    }

    private fun toVariablesJson(variables: Map<String, String>): String {
        if (variables.isEmpty()) {
            return "{}"
        }
        return objectMapper.writeValueAsString(variables)
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
