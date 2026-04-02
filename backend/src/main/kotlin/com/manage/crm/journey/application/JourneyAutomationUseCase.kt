package com.manage.crm.journey.application

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.action.application.ActionChannel
import com.manage.crm.action.application.ActionDispatchIn
import com.manage.crm.action.application.ActionDispatchService
import com.manage.crm.action.application.ActionDispatchStatus
import com.manage.crm.event.application.port.query.EventReadPort
import com.manage.crm.event.domain.Event
import com.manage.crm.journey.application.automation.condition.ConditionEvaluator
import com.manage.crm.journey.application.automation.condition.ConditionExpressionResolver
import com.manage.crm.journey.application.automation.condition.ConditionTriggerHandler
import com.manage.crm.journey.application.automation.segment.SegmentTriggerHandler
import com.manage.crm.journey.application.dto.JourneyAutomationUseCaseIn
import com.manage.crm.journey.application.dto.JourneyExecutionHistoryStatus
import com.manage.crm.journey.application.dto.JourneyExecutionStatus
import com.manage.crm.journey.application.dto.JourneyStepType
import com.manage.crm.journey.application.dto.JourneyTriggerType
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyExecution
import com.manage.crm.journey.domain.JourneyExecutionHistory
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.JourneyStepDeduplication
import com.manage.crm.journey.domain.repository.JourneyExecutionHistoryRepository
import com.manage.crm.journey.domain.repository.JourneyExecutionRepository
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneySegmentCountStateRepository
import com.manage.crm.journey.domain.repository.JourneySegmentUserStateRepository
import com.manage.crm.journey.domain.repository.JourneyStepDeduplicationRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import com.manage.crm.journey.exception.InvalidJourneyStepException
import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.user.application.port.query.UserReadPort
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private enum class JourneyStepExecutionDecision {
    CONTINUE,
    STOP,
}

/**
 * UC-JOURNEY-007
 * Orchestrates journey automation triggers from queue events.
 *
 * Input: queue-translated automation trigger payload containing event or changed user ids.
 * Success: dispatches event/segment-triggered journey execution flow without changing queue runtime behavior.
 */
@Component
class JourneyAutomationUseCase(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val journeyExecutionRepository: JourneyExecutionRepository,
    private val journeyExecutionHistoryRepository: JourneyExecutionHistoryRepository,
    private val journeyStepDeduplicationRepository: JourneyStepDeduplicationRepository,
    private val journeySegmentUserStateRepository: JourneySegmentUserStateRepository,
    private val journeySegmentCountStateRepository: JourneySegmentCountStateRepository,
    private val segmentReadPort: SegmentReadPort,
    private val eventReadPort: EventReadPort,
    private val actionDispatchService: ActionDispatchService,
    private val userReadPort: UserReadPort,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}
    private val conditionEvaluator = ConditionEvaluator()
    private val conditionTriggerHandler =
        ConditionTriggerHandler(
            journeyRepository = journeyRepository,
            journeyStepRepository = journeyStepRepository,
            conditionExpressionResolver = ConditionExpressionResolver(),
            conditionEvaluator = conditionEvaluator,
        )
    private val segmentTriggerHandler =
        SegmentTriggerHandler(
            journeyRepository = journeyRepository,
            journeySegmentUserStateRepository = journeySegmentUserStateRepository,
            journeySegmentCountStateRepository = journeySegmentCountStateRepository,
            segmentReadPort = segmentReadPort,
            eventReadPort = eventReadPort,
            userReadPort = userReadPort,
            objectMapper = objectMapper,
        )

    suspend fun execute(useCaseIn: JourneyAutomationUseCaseIn) {
        val event = useCaseIn.event
        if (event != null) {
            onEvent(event)
            return
        }
        onSegmentContextChanged(useCaseIn.changedUserIds)
    }

    private suspend fun onEvent(event: Event) {
        val eventJourneys =
            journeyRepository
                .findAllByTriggerTypeAndTriggerEventNameAndActiveTrue(
                    triggerType = JourneyTriggerType.EVENT.name,
                    triggerEventName = event.name,
                ).toList()

        val eventId = requireNotNull(event.id) { "Event id cannot be null" }
        eventJourneys.forEach { journey ->
            val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
            val triggerKey = "$journeyId:$eventId:${event.userId}"
            executeJourney(journey, event, triggerKey)
        }

        conditionTriggerHandler.processConditionTriggeredJourneys(event) { journey, triggerEvent, triggerKey ->
            executeJourney(journey, triggerEvent, triggerKey)
        }
    }

    private suspend fun onSegmentContextChanged(changedUserIds: List<Long>? = null) {
        segmentTriggerHandler.processSegmentTriggeredJourneys(changedUserIds) { journey, triggerEvent, triggerKey ->
            executeJourney(journey, triggerEvent, triggerKey)
        }
    }

    private suspend fun executeJourney(
        journey: Journey,
        event: Event,
        triggerKey: String,
    ) {
        val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
        val eventId = requireNotNull(event.id) { "Event id cannot be null" }

        val execution =
            runCatching {
                journeyExecutionRepository.save(
                    JourneyExecution.new(
                        journeyId = journeyId,
                        eventId = eventId,
                        userId = event.userId,
                        status = JourneyExecutionStatus.RUNNING.name,
                        currentStepOrder = 0,
                        triggerKey = triggerKey,
                        startedAt = LocalDateTime.now(),
                    ),
                )
            }.getOrElse { error ->
                if (error is DataIntegrityViolationException) {
                    log.debug { "Skip duplicated journey execution by triggerKey=$triggerKey" }
                    return
                }
                throw error
            }

        runCatching {
            executeJourneySteps(execution, event)

            execution.status = JourneyExecutionStatus.SUCCESS.name
            execution.completedAt = LocalDateTime.now()
            execution.lastError = null
            journeyExecutionRepository.save(execution)
        }.onFailure { error ->
            log.error(error) { "Journey execution failed: journeyId=$journeyId, eventId=$eventId, userId=${event.userId}" }
            execution.status = JourneyExecutionStatus.FAILED.name
            execution.completedAt = LocalDateTime.now()
            execution.lastError = error.message
            journeyExecutionRepository.save(execution)
        }
    }

    private suspend fun executeJourneySteps(
        execution: JourneyExecution,
        event: Event,
    ) {
        val journeyId = execution.journeyId
        val executionId = requireNotNull(execution.id) { "JourneyExecution id cannot be null" }

        val steps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()

        for (step in steps) {
            execution.currentStepOrder = step.stepOrder
            journeyExecutionRepository.save(execution)

            val decision =
                when (JourneyStepType.from(step.stepType)) {
                    JourneyStepType.BRANCH -> executeBranchStep(executionId, step, event)
                    JourneyStepType.DELAY -> executeDelayStep(executionId, step, event)
                    JourneyStepType.ACTION -> executeActionStep(executionId, step, event)
                }

            if (decision == JourneyStepExecutionDecision.STOP) {
                break
            }
        }
    }

    private suspend fun executeBranchStep(
        executionId: Long,
        step: JourneyStep,
        event: Event,
    ): JourneyStepExecutionDecision {
        val stepId = requireNotNull(step.id) { "JourneyStep id cannot be null" }
        val conditionMatched = conditionEvaluator.evaluate(step.conditionExpression, event)

        if (!conditionMatched) {
            saveHistory(
                executionId = executionId,
                stepId = stepId,
                status = JourneyExecutionHistoryStatus.SKIPPED,
                attempt = 0,
                message = "Branch condition is not matched",
                idempotencyKey = null,
            )
            return JourneyStepExecutionDecision.STOP
        }

        saveHistory(
            executionId = executionId,
            stepId = stepId,
            status = JourneyExecutionHistoryStatus.SUCCESS,
            attempt = 1,
            message = "Branch condition matched",
            idempotencyKey = null,
        )
        return JourneyStepExecutionDecision.CONTINUE
    }

    private suspend fun executeDelayStep(
        executionId: Long,
        step: JourneyStep,
        event: Event,
    ): JourneyStepExecutionDecision {
        val stepId = requireNotNull(step.id) { "JourneyStep id cannot be null" }
        val conditionMatched = conditionEvaluator.evaluate(step.conditionExpression, event)
        if (!conditionMatched) {
            saveHistory(
                executionId = executionId,
                stepId = stepId,
                status = JourneyExecutionHistoryStatus.SKIPPED,
                attempt = 0,
                message = "Delay condition is not matched",
                idempotencyKey = null,
            )
            return JourneyStepExecutionDecision.CONTINUE
        }

        val boundedDelayMillis = (step.delayMillis ?: 0L).coerceAtLeast(0L).coerceAtMost(60_000L)
        if (boundedDelayMillis > 0) {
            delay(boundedDelayMillis)
        }

        saveHistory(
            executionId = executionId,
            stepId = stepId,
            status = JourneyExecutionHistoryStatus.SUCCESS,
            attempt = 1,
            message = "Delay completed: ${boundedDelayMillis}ms",
            idempotencyKey = null,
        )
        return JourneyStepExecutionDecision.CONTINUE
    }

    private suspend fun executeActionStep(
        executionId: Long,
        step: JourneyStep,
        event: Event,
    ): JourneyStepExecutionDecision {
        val stepId = requireNotNull(step.id) { "JourneyStep id cannot be null" }

        if (!conditionEvaluator.evaluate(step.conditionExpression, event)) {
            saveHistory(
                executionId = executionId,
                stepId = stepId,
                status = JourneyExecutionHistoryStatus.SKIPPED,
                attempt = 0,
                message = "Action condition is not matched",
                idempotencyKey = null,
            )
            return JourneyStepExecutionDecision.CONTINUE
        }

        val dedupeKey = "$executionId:$stepId"
        if (!acquireStepDeduplicationKey(dedupeKey)) {
            saveHistory(
                executionId = executionId,
                stepId = stepId,
                status = JourneyExecutionHistoryStatus.SKIPPED_DUPLICATE,
                attempt = 0,
                message = "Step execution is deduplicated by idempotency key",
                idempotencyKey = dedupeKey,
            )
            return JourneyStepExecutionDecision.CONTINUE
        }

        val channel =
            step.channel?.let { ActionChannel.from(it) }
                ?: throw InvalidJourneyStepException("channel is required for ACTION step")
        val destination =
            step.destination
                ?: throw InvalidJourneyStepException("destination is required for ACTION step")
        val body =
            step.body
                ?: throw InvalidJourneyStepException("body is required for ACTION step")

        val retryCount = step.retryCount.coerceAtLeast(0)
        val variables = resolveStepVariables(step.variablesJson, event)

        repeat(retryCount + 1) { retryIndex ->
            val attempt = retryIndex + 1
            saveHistory(
                executionId = executionId,
                stepId = stepId,
                status = JourneyExecutionHistoryStatus.RUNNING,
                attempt = attempt,
                message = "Action dispatch attempt started",
                idempotencyKey = dedupeKey,
            )

            val dispatchResult =
                actionDispatchService.dispatch(
                    ActionDispatchIn(
                        channel = channel,
                        destination = destination,
                        subject = step.subject,
                        body = body,
                        variables = variables,
                        campaignId = null,
                        journeyExecutionId = executionId,
                    ),
                )

            if (dispatchResult.status == ActionDispatchStatus.SUCCESS) {
                saveHistory(
                    executionId = executionId,
                    stepId = stepId,
                    status = JourneyExecutionHistoryStatus.SUCCESS,
                    attempt = attempt,
                    message = "Action dispatch succeeded",
                    idempotencyKey = dedupeKey,
                )
                return JourneyStepExecutionDecision.CONTINUE
            }

            if (attempt <= retryCount) {
                saveHistory(
                    executionId = executionId,
                    stepId = stepId,
                    status = JourneyExecutionHistoryStatus.RETRYING,
                    attempt = attempt,
                    message = dispatchResult.errorMessage ?: "Action dispatch failed and will retry",
                    idempotencyKey = dedupeKey,
                )
                delay(200)
            } else {
                saveHistory(
                    executionId = executionId,
                    stepId = stepId,
                    status = JourneyExecutionHistoryStatus.FAILED,
                    attempt = attempt,
                    message = dispatchResult.errorMessage ?: "Action dispatch failed",
                    idempotencyKey = dedupeKey,
                )
                throw IllegalStateException(dispatchResult.errorMessage ?: "Action dispatch failed")
            }
        }

        return JourneyStepExecutionDecision.CONTINUE
    }

    private suspend fun saveHistory(
        executionId: Long,
        stepId: Long,
        status: JourneyExecutionHistoryStatus,
        attempt: Int,
        message: String?,
        idempotencyKey: String?,
    ) {
        journeyExecutionHistoryRepository.save(
            JourneyExecutionHistory.new(
                journeyExecutionId = executionId,
                journeyStepId = stepId,
                status = status.name,
                attempt = attempt,
                message = message,
                idempotencyKey = idempotencyKey,
            ),
        )
    }

    private suspend fun acquireStepDeduplicationKey(idempotencyKey: String): Boolean =
        runCatching {
            journeyStepDeduplicationRepository.save(JourneyStepDeduplication.new(idempotencyKey))
            true
        }.getOrElse { error ->
            if (error is DataIntegrityViolationException) {
                false
            } else {
                throw error
            }
        }

    private suspend fun resolveStepVariables(
        variablesJson: String?,
        event: Event,
    ): Map<String, String> {
        val eventId = requireNotNull(event.id) { "Event id cannot be null" }
        val eventVariables =
            buildMap {
                put("eventId", eventId.toString())
                put("userId", event.userId.toString())
                put("eventName", event.name)
                event.properties.value.forEach {
                    put("event.${it.key}", it.value)
                }
            }
        val userVariables =
            userReadPort
                .findAllByIdIn(listOf(event.userId))
                .firstOrNull()
                ?.let { user ->
                    buildMap {
                        put("user.id", event.userId.toString())
                        put("user.externalId", user.externalId)
                        val userAttributesNode = runCatching { objectMapper.readTree(user.userAttributesJson) }.getOrNull()
                        userAttributesNode
                            ?.get(RequiredUserAttributeKey.EMAIL.value)
                            ?.asText()
                            ?.takeIf { it.isNotBlank() }
                            ?.let { put("user.email", it) }
                        userAttributesNode
                            ?.get(RequiredUserAttributeKey.NAME.value)
                            ?.asText()
                            ?.takeIf { it.isNotBlank() }
                            ?.let { put("user.name", it) }
                    }
                }
                ?: mapOf("user.id" to event.userId.toString())

        val stepVariables =
            if (variablesJson.isNullOrBlank()) {
                emptyMap()
            } else {
                runCatching {
                    objectMapper.readValue(variablesJson, object : TypeReference<Map<String, String>>() {})
                }.getOrElse {
                    emptyMap()
                }
            }

        return eventVariables + userVariables + stepVariables
    }
}
