package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.application.dto.JourneyLifecycleStatus
import com.manage.crm.journey.application.dto.JourneySegmentTriggerEventType
import com.manage.crm.journey.application.dto.JourneyStepType
import com.manage.crm.journey.application.dto.JourneyTriggerType
import com.manage.crm.journey.application.dto.PutJourneyUseCaseIn
import com.manage.crm.journey.application.dto.PutJourneyUseCaseOut
import com.manage.crm.journey.application.dto.toJourneyDto
import com.manage.crm.journey.application.dto.toJourneyStepDto
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.repository.JourneyExecutionHistoryRepository
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import com.manage.crm.journey.exception.InvalidJourneyException
import com.manage.crm.journey.exception.InvalidJourneyStepException
import com.manage.crm.support.exception.NotFoundByIdException
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * UC-JOURNEY-002
 * Updates a journey and reconciles its step definitions without breaking execution history FK links.
 *
 * Input: target journey id and full replacement definition for trigger/steps.
 * Success: updates journey and step set while preserving history-linked step constraints.
 */
@Component
class PutJourneyUseCase(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val journeyExecutionHistoryRepository: JourneyExecutionHistoryRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    suspend fun execute(useCaseIn: PutJourneyUseCaseIn): PutJourneyUseCaseOut {
        validate(useCaseIn)

        val journey =
            journeyRepository.findById(useCaseIn.journeyId)
                ?: throw NotFoundByIdException("Journey", useCaseIn.journeyId)

        if (journey.lifecycleStatus == JourneyLifecycleStatus.ARCHIVED.name) {
            throw InvalidJourneyException("Archived journey cannot be updated")
        }

        journey.name = useCaseIn.name
        journey.triggerType = useCaseIn.triggerType.name
        journey.triggerEventName = useCaseIn.triggerEventName
        journey.triggerSegmentId = useCaseIn.triggerSegmentId
        journey.triggerSegmentEvent = useCaseIn.triggerSegmentEvent?.name
        journey.triggerSegmentWatchFields = toTriggerSegmentWatchFieldsJson(useCaseIn.triggerSegmentWatchFields)
        journey.triggerSegmentCountThreshold = useCaseIn.triggerSegmentCountThreshold
        journey.active = useCaseIn.active
        journey.lifecycleStatus =
            when {
                useCaseIn.active -> JourneyLifecycleStatus.ACTIVE.name
                journey.lifecycleStatus == JourneyLifecycleStatus.DRAFT.name -> JourneyLifecycleStatus.DRAFT.name
                else -> JourneyLifecycleStatus.PAUSED.name
            }
        journey.version = journey.version.coerceAtLeast(1) + 1

        val savedJourney = journeyRepository.save(journey)
        val journeyId = requireNotNull(savedJourney.id) { "Journey id cannot be null" }

        val existingSteps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()
        val existingStepByOrder = existingSteps.associateBy { it.stepOrder }.toMutableMap()
        val incomingStepOrders = mutableSetOf<Int>()

        val savedSteps =
            useCaseIn.steps
                .sortedBy { it.stepOrder }
                .map { step ->
                    incomingStepOrders += step.stepOrder
                    val existingStep = existingStepByOrder.remove(step.stepOrder)

                    if (existingStep != null) {
                        existingStep.stepType = step.stepType.name
                        existingStep.channel = step.channel
                        existingStep.destination = step.destination
                        existingStep.subject = step.subject
                        existingStep.body = step.body
                        existingStep.variablesJson = toVariablesJson(step.variables)
                        existingStep.delayMillis = step.delayMillis
                        existingStep.conditionExpression = step.conditionExpression
                        existingStep.retryCount = step.retryCount.coerceAtLeast(0)
                        return@map journeyStepRepository.save(existingStep)
                    }

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

        val removableSteps =
            existingStepByOrder.values
                .filter { !incomingStepOrders.contains(it.stepOrder) }
        removableSteps.forEach { step ->
            val stepId = step.id
            if (stepId != null && journeyExecutionHistoryRepository.existsByJourneyStepId(stepId)) {
                throw InvalidJourneyStepException(
                    "Cannot remove stepOrder=${step.stepOrder} because execution history already exists",
                )
            }
            journeyStepRepository.delete(step)
        }

        val stepDtos = savedSteps.map { it.toJourneyStepDto(objectMapper) }
        return PutJourneyUseCaseOut(savedJourney.toJourneyDto(stepDtos, objectMapper))
    }

    private fun validate(useCaseIn: PutJourneyUseCaseIn) {
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

            JourneyTriggerType.CONDITION -> Unit
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
