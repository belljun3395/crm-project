package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import org.springframework.stereotype.Service

/**
 * Creates a journey with trigger metadata and ordered execution steps.
 */
@Service
class PostJourneyUseCase(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val objectMapper: ObjectMapper
) {
    suspend fun execute(useCaseIn: PostJourneyIn): JourneyDto {
        validate(useCaseIn)

        val savedJourney = journeyRepository.save(
            Journey.new(
                name = useCaseIn.name,
                triggerType = useCaseIn.triggerType.name,
                triggerEventName = useCaseIn.triggerEventName,
                triggerSegmentId = useCaseIn.triggerSegmentId,
                triggerSegmentEvent = useCaseIn.triggerSegmentEvent?.name,
                triggerSegmentWatchFields = toTriggerSegmentWatchFieldsJson(useCaseIn.triggerSegmentWatchFields),
                triggerSegmentCountThreshold = useCaseIn.triggerSegmentCountThreshold,
                active = useCaseIn.active,
                lifecycleStatus = if (useCaseIn.active) JourneyLifecycleStatus.ACTIVE.name else JourneyLifecycleStatus.DRAFT.name,
                version = 1
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

        return assembleJourneyDto(savedJourney, savedSteps, objectMapper)
    }

    private fun validate(useCaseIn: PostJourneyIn) {
        if (useCaseIn.name.isBlank()) {
            throw IllegalArgumentException("Journey name is required")
        }

        if (useCaseIn.steps.isEmpty()) {
            throw IllegalArgumentException("Journey steps are required")
        }
        if (useCaseIn.steps.any { it.stepOrder <= 0 }) {
            throw IllegalArgumentException("stepOrder must be greater than 0")
        }
        if (useCaseIn.steps.groupingBy { it.stepOrder }.eachCount().any { it.value > 1 }) {
            throw IllegalArgumentException("stepOrder must be unique")
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
                val segmentEvent = useCaseIn.triggerSegmentEvent
                    ?: throw IllegalArgumentException("triggerSegmentEvent is required for SEGMENT trigger")
                when (segmentEvent) {
                    JourneySegmentTriggerEventType.ENTER,
                    JourneySegmentTriggerEventType.EXIT -> Unit

                    JourneySegmentTriggerEventType.UPDATE -> {
                        if (useCaseIn.triggerSegmentWatchFields.isEmpty()) {
                            throw IllegalArgumentException("triggerSegmentWatchFields is required for SEGMENT UPDATE trigger")
                        }
                    }

                    JourneySegmentTriggerEventType.COUNT_REACHED,
                    JourneySegmentTriggerEventType.COUNT_DROPPED -> {
                        val threshold = useCaseIn.triggerSegmentCountThreshold
                        if (threshold == null || threshold <= 0L) {
                            throw IllegalArgumentException("triggerSegmentCountThreshold must be greater than 0 for SEGMENT COUNT trigger")
                        }
                    }
                }
            }

            JourneyTriggerType.CONDITION -> {
                val triggerExpression = useCaseIn.triggerEventName?.takeIf { it.isNotBlank() }
                val branchExpressions = useCaseIn.steps
                    .filter { it.stepType == JourneyStepType.BRANCH && !it.conditionExpression.isNullOrBlank() }
                    .map { requireNotNull(it.conditionExpression).trim() }

                if (triggerExpression.isNullOrBlank() && branchExpressions.isEmpty()) {
                    throw IllegalArgumentException(
                        "CONDITION trigger requires triggerEventName(condition expression) or BRANCH step conditionExpression"
                    )
                }

                triggerExpression?.let { validateConditionExpression(it) }
                branchExpressions.forEach { validateConditionExpression(it) }
            }
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

    private fun toTriggerSegmentWatchFieldsJson(fields: List<String>): String? {
        if (fields.isEmpty()) {
            return null
        }
        return objectMapper.writeValueAsString(fields)
    }
    private fun validateConditionExpression(expression: String) {
        val raw = expression.trim()
        val operator = when {
            raw.contains("==") -> "=="
            raw.contains("!=") -> "!="
            else -> throw IllegalArgumentException("Unsupported condition expression: $expression")
        }

        val parts = raw.split(operator, limit = 2)
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid condition expression: $expression")
        }

        val left = parts[0].trim()
        val right = parts[1].trim().trim('"').trim('\'')
        if (!left.startsWith("event.") || left.removePrefix("event.").isBlank() || right.isBlank()) {
            throw IllegalArgumentException("Invalid condition expression: $expression")
        }
    }
}
