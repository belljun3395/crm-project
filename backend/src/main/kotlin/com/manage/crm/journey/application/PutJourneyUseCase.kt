package com.manage.crm.journey.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import com.manage.crm.support.exception.NotFoundByIdException
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class PutJourneyStepIn(
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

data class PutJourneyIn(
    val journeyId: Long,
    val name: String,
    val triggerType: JourneyTriggerType,
    val triggerEventName: String?,
    val triggerSegmentId: Long?,
    val triggerSegmentEvent: JourneySegmentTriggerEventType?,
    val triggerSegmentWatchFields: List<String>,
    val triggerSegmentCountThreshold: Long?,
    val active: Boolean,
    val steps: List<PutJourneyStepIn>
)

@Service
class PutJourneyUseCase(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    suspend fun execute(useCaseIn: PutJourneyIn): JourneyDto {
        validate(useCaseIn)

        val journey = journeyRepository.findById(useCaseIn.journeyId)
            ?: throw NotFoundByIdException("Journey", useCaseIn.journeyId)

        if (journey.lifecycleStatus == JourneyLifecycleStatus.ARCHIVED.name) {
            throw IllegalArgumentException("Archived journey cannot be updated")
        }

        journey.name = useCaseIn.name
        journey.triggerType = useCaseIn.triggerType.name
        journey.triggerEventName = useCaseIn.triggerEventName
        journey.triggerSegmentId = useCaseIn.triggerSegmentId
        journey.triggerSegmentEvent = useCaseIn.triggerSegmentEvent?.name
        journey.triggerSegmentWatchFields = toTriggerSegmentWatchFieldsJson(useCaseIn.triggerSegmentWatchFields)
        journey.triggerSegmentCountThreshold = useCaseIn.triggerSegmentCountThreshold
        journey.active = useCaseIn.active
        journey.lifecycleStatus = if (useCaseIn.active) JourneyLifecycleStatus.ACTIVE.name else JourneyLifecycleStatus.PAUSED.name
        journey.version = journey.version.coerceAtLeast(1) + 1

        val savedJourney = journeyRepository.save(journey)
        val journeyId = requireNotNull(savedJourney.id) { "Journey id cannot be null" }

        val existingSteps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()
        existingSteps.forEach { journeyStepRepository.delete(it) }

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

    private fun validate(useCaseIn: PutJourneyIn) {
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

    private fun toTriggerSegmentWatchFieldsJson(fields: List<String>): String? {
        if (fields.isEmpty()) {
            return null
        }
        return objectMapper.writeValueAsString(fields)
    }
}
