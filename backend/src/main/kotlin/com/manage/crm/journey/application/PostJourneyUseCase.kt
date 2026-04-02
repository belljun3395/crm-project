package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.application.dto.JourneyLifecycleStatus
import com.manage.crm.journey.application.dto.JourneySegmentTriggerEventType
import com.manage.crm.journey.application.dto.JourneyStepType
import com.manage.crm.journey.application.dto.JourneyTriggerType
import com.manage.crm.journey.application.dto.PostJourneyUseCaseIn
import com.manage.crm.journey.application.dto.PostJourneyUseCaseOut
import com.manage.crm.journey.application.dto.toJourneyDto
import com.manage.crm.journey.application.dto.toJourneyStepDto
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import com.manage.crm.journey.exception.InvalidJourneyException
import com.manage.crm.journey.exception.InvalidJourneyStepException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * UC-JOURNEY-001
 * Creates a journey with trigger metadata and ordered execution steps.
 *
 * Input: journey name, trigger configuration, and ordered step definitions.
 * Success: persists journey/steps and returns a journey DTO with normalized lifecycle metadata.
 */
@Component
class PostJourneyUseCase(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    suspend fun execute(useCaseIn: PostJourneyUseCaseIn): PostJourneyUseCaseOut {
        validate(useCaseIn)

        val savedJourney =
            journeyRepository.save(
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
                    version = 1,
                ),
            )

        val journeyId = requireNotNull(savedJourney.id) { "Journey id cannot be null" }
        val savedSteps =
            useCaseIn.steps
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
                            retryCount = step.retryCount.coerceAtLeast(0),
                        ),
                    )
                }

        val stepDtos = savedSteps.map { it.toJourneyStepDto(objectMapper) }
        return PostJourneyUseCaseOut(savedJourney.toJourneyDto(stepDtos, objectMapper))
    }

    private fun validate(useCaseIn: PostJourneyUseCaseIn) {
        if (useCaseIn.name.isBlank()) {
            throw InvalidJourneyException("Journey name is required")
        }

        if (useCaseIn.steps.isEmpty()) {
            throw InvalidJourneyException("Journey steps are required")
        }
        if (useCaseIn.steps.any { it.stepOrder <= 0 }) {
            throw InvalidJourneyStepException("stepOrder must be greater than 0")
        }
        if (useCaseIn.steps
                .groupingBy { it.stepOrder }
                .eachCount()
                .any { it.value > 1 }
        ) {
            throw InvalidJourneyStepException("stepOrder must be unique")
        }

        when (useCaseIn.triggerType) {
            JourneyTriggerType.EVENT -> {
                if (useCaseIn.triggerEventName.isNullOrBlank()) {
                    throw InvalidJourneyException("triggerEventName is required for EVENT trigger")
                }
            }

            JourneyTriggerType.SEGMENT -> {
                if (useCaseIn.triggerSegmentId == null) {
                    throw InvalidJourneyException("triggerSegmentId is required for SEGMENT trigger")
                }
                val segmentEvent =
                    useCaseIn.triggerSegmentEvent
                        ?: throw InvalidJourneyException("triggerSegmentEvent is required for SEGMENT trigger")
                when (segmentEvent) {
                    JourneySegmentTriggerEventType.ENTER,
                    JourneySegmentTriggerEventType.EXIT,
                    -> Unit

                    JourneySegmentTriggerEventType.UPDATE -> {
                        if (useCaseIn.triggerSegmentWatchFields.isEmpty()) {
                            throw InvalidJourneyException("triggerSegmentWatchFields is required for SEGMENT UPDATE trigger")
                        }
                    }

                    JourneySegmentTriggerEventType.COUNT_REACHED,
                    JourneySegmentTriggerEventType.COUNT_DROPPED,
                    -> {
                        val threshold = useCaseIn.triggerSegmentCountThreshold
                        if (threshold == null || threshold <= 0L) {
                            throw InvalidJourneyException("triggerSegmentCountThreshold must be greater than 0 for SEGMENT COUNT trigger")
                        }
                    }
                }
            }

            JourneyTriggerType.CONDITION -> {
                if (useCaseIn.triggerEventName.isNullOrBlank()) {
                    throw InvalidJourneyException("triggerEventName is required for CONDITION trigger")
                }
                if (useCaseIn.steps.none { it.stepType == JourneyStepType.BRANCH }) {
                    throw InvalidJourneyException("At least one BRANCH step is required for CONDITION trigger")
                }
            }
        }

        useCaseIn.steps.forEach { step ->
            when (step.stepType) {
                JourneyStepType.ACTION -> {
                    if (step.channel.isNullOrBlank()) {
                        throw InvalidJourneyStepException("channel is required for ACTION step")
                    }
                    if (step.destination.isNullOrBlank()) {
                        throw InvalidJourneyStepException("destination is required for ACTION step")
                    }
                    if (step.body.isNullOrBlank()) {
                        throw InvalidJourneyStepException("body is required for ACTION step")
                    }
                }

                JourneyStepType.DELAY -> {
                    if (step.delayMillis == null || step.delayMillis < 0) {
                        throw InvalidJourneyStepException("delayMillis must be zero or greater for DELAY step")
                    }
                }

                JourneyStepType.BRANCH -> {
                    if (step.conditionExpression.isNullOrBlank()) {
                        throw InvalidJourneyStepException("conditionExpression is required for BRANCH step")
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
}
