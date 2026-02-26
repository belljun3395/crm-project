package com.manage.crm.journey.application

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.action.application.ActionChannel
import com.manage.crm.action.application.ActionDispatchIn
import com.manage.crm.action.application.ActionDispatchService
import com.manage.crm.action.application.ActionDispatchStatus
import com.manage.crm.event.domain.Event
import com.manage.crm.journey.domain.JourneyExecution
import com.manage.crm.journey.domain.JourneyExecutionHistory
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.JourneyStepDeduplication
import com.manage.crm.journey.domain.repository.JourneyExecutionHistoryRepository
import com.manage.crm.journey.domain.repository.JourneyExecutionRepository
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneyStepDeduplicationRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

private enum class JourneyStepExecutionDecision {
    CONTINUE,
    STOP
}

@Service
class JourneyAutomationService(
    private val journeyRepository: JourneyRepository,
    private val journeyStepRepository: JourneyStepRepository,
    private val journeyExecutionRepository: JourneyExecutionRepository,
    private val journeyExecutionHistoryRepository: JourneyExecutionHistoryRepository,
    private val journeyStepDeduplicationRepository: JourneyStepDeduplicationRepository,
    private val actionDispatchService: ActionDispatchService,
    private val objectMapper: ObjectMapper
) {
    private val log = KotlinLogging.logger {}

    suspend fun onEvent(event: Event) {
        val eventId = requireNotNull(event.id) { "Event id cannot be null" }

        val journeys = journeyRepository
            .findAllByTriggerTypeAndTriggerEventNameAndActiveTrue(
                triggerType = JourneyTriggerType.EVENT.name,
                triggerEventName = event.name
            )
            .toList()

        journeys.forEach { journey ->
            val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
            val triggerKey = "$journeyId:$eventId:${event.userId}"

            if (journeyExecutionRepository.findByTriggerKey(triggerKey) != null) {
                log.debug { "Skip duplicated journey execution by triggerKey=$triggerKey" }
                return@forEach
            }

            val execution = journeyExecutionRepository.save(
                JourneyExecution.new(
                    journeyId = journeyId,
                    eventId = eventId,
                    userId = event.userId,
                    status = JourneyExecutionStatus.RUNNING.name,
                    currentStepOrder = 0,
                    triggerKey = triggerKey,
                    startedAt = LocalDateTime.now()
                )
            )

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
    }

    private suspend fun executeJourneySteps(execution: JourneyExecution, event: Event) {
        val journeyId = execution.journeyId
        val executionId = requireNotNull(execution.id) { "JourneyExecution id cannot be null" }
        val eventId = requireNotNull(event.id) { "Event id cannot be null" }

        val steps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()

        for (step in steps) {
            execution.currentStepOrder = step.stepOrder
            journeyExecutionRepository.save(execution)

            val decision = when (JourneyStepType.from(step.stepType)) {
                JourneyStepType.BRANCH -> executeBranchStep(executionId, step, event)
                JourneyStepType.DELAY -> executeDelayStep(executionId, step, event)
                JourneyStepType.ACTION -> executeActionStep(executionId, step, eventId, event)
            }

            if (decision == JourneyStepExecutionDecision.STOP) {
                break
            }
        }
    }

    private suspend fun executeBranchStep(
        executionId: Long,
        step: JourneyStep,
        event: Event
    ): JourneyStepExecutionDecision {
        val stepId = requireNotNull(step.id) { "JourneyStep id cannot be null" }
        val conditionMatched = evaluateCondition(step.conditionExpression, event)

        if (!conditionMatched) {
            saveHistory(
                executionId = executionId,
                stepId = stepId,
                status = JourneyExecutionHistoryStatus.SKIPPED,
                attempt = 0,
                message = "Branch condition is not matched",
                idempotencyKey = null
            )
            return JourneyStepExecutionDecision.STOP
        }

        saveHistory(
            executionId = executionId,
            stepId = stepId,
            status = JourneyExecutionHistoryStatus.SUCCESS,
            attempt = 1,
            message = "Branch condition matched",
            idempotencyKey = null
        )
        return JourneyStepExecutionDecision.CONTINUE
    }

    private suspend fun executeDelayStep(
        executionId: Long,
        step: JourneyStep,
        event: Event
    ): JourneyStepExecutionDecision {
        val stepId = requireNotNull(step.id) { "JourneyStep id cannot be null" }
        val conditionMatched = evaluateCondition(step.conditionExpression, event)
        if (!conditionMatched) {
            saveHistory(
                executionId = executionId,
                stepId = stepId,
                status = JourneyExecutionHistoryStatus.SKIPPED,
                attempt = 0,
                message = "Delay condition is not matched",
                idempotencyKey = null
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
            idempotencyKey = null
        )
        return JourneyStepExecutionDecision.CONTINUE
    }

    private suspend fun executeActionStep(
        executionId: Long,
        step: JourneyStep,
        eventId: Long,
        event: Event
    ): JourneyStepExecutionDecision {
        val stepId = requireNotNull(step.id) { "JourneyStep id cannot be null" }

        if (!evaluateCondition(step.conditionExpression, event)) {
            saveHistory(
                executionId = executionId,
                stepId = stepId,
                status = JourneyExecutionHistoryStatus.SKIPPED,
                attempt = 0,
                message = "Action condition is not matched",
                idempotencyKey = null
            )
            return JourneyStepExecutionDecision.CONTINUE
        }

        val dedupeKey = "$eventId:${event.userId}:$stepId"
        if (!acquireStepDeduplicationKey(dedupeKey)) {
            saveHistory(
                executionId = executionId,
                stepId = stepId,
                status = JourneyExecutionHistoryStatus.SKIPPED_DUPLICATE,
                attempt = 0,
                message = "Step execution is deduplicated by idempotency key",
                idempotencyKey = dedupeKey
            )
            return JourneyStepExecutionDecision.CONTINUE
        }

        val channel = step.channel?.let { ActionChannel.from(it) }
            ?: throw IllegalArgumentException("channel is required for ACTION step")
        val destination = step.destination
            ?: throw IllegalArgumentException("destination is required for ACTION step")
        val body = step.body
            ?: throw IllegalArgumentException("body is required for ACTION step")

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
                idempotencyKey = dedupeKey
            )

            val dispatchResult = actionDispatchService.dispatch(
                ActionDispatchIn(
                    channel = channel,
                    destination = destination,
                    subject = step.subject,
                    body = body,
                    variables = variables,
                    campaignId = null,
                    journeyExecutionId = executionId
                )
            )

            if (dispatchResult.status == ActionDispatchStatus.SUCCESS) {
                saveHistory(
                    executionId = executionId,
                    stepId = stepId,
                    status = JourneyExecutionHistoryStatus.SUCCESS,
                    attempt = attempt,
                    message = "Action dispatch succeeded",
                    idempotencyKey = dedupeKey
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
                    idempotencyKey = dedupeKey
                )
                delay(200)
            } else {
                saveHistory(
                    executionId = executionId,
                    stepId = stepId,
                    status = JourneyExecutionHistoryStatus.FAILED,
                    attempt = attempt,
                    message = dispatchResult.errorMessage ?: "Action dispatch failed",
                    idempotencyKey = dedupeKey
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
        idempotencyKey: String?
    ) {
        journeyExecutionHistoryRepository.save(
            JourneyExecutionHistory.new(
                journeyExecutionId = executionId,
                journeyStepId = stepId,
                status = status.name,
                attempt = attempt,
                message = message,
                idempotencyKey = idempotencyKey
            )
        )
    }

    private suspend fun acquireStepDeduplicationKey(idempotencyKey: String): Boolean {
        return runCatching {
            journeyStepDeduplicationRepository.save(JourneyStepDeduplication.new(idempotencyKey))
            true
        }.getOrElse { error ->
            if (error is DataIntegrityViolationException) {
                false
            } else {
                throw error
            }
        }
    }

    private fun evaluateCondition(conditionExpression: String?, event: Event): Boolean {
        if (conditionExpression.isNullOrBlank()) {
            return true
        }

        val expression = conditionExpression.trim()
        val operator = when {
            expression.contains("==") -> "=="
            expression.contains("!=") -> "!="
            else -> throw IllegalArgumentException("Unsupported condition expression: $conditionExpression")
        }

        val parts = expression.split(operator, limit = 2)
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid condition expression: $conditionExpression")
        }

        val left = parts[0].trim()
        val right = parts[1].trim().trim('"').trim('\'')

        if (!left.startsWith("event.")) {
            throw IllegalArgumentException("Condition left operand must start with event.: $conditionExpression")
        }

        val key = left.removePrefix("event.")
        val actual = event.properties.value.firstOrNull { it.key == key }?.value

        return when (operator) {
            "==" -> actual == right
            "!=" -> actual != right
            else -> false
        }
    }

    private fun resolveStepVariables(variablesJson: String?, event: Event): Map<String, String> {
        val eventId = requireNotNull(event.id) { "Event id cannot be null" }
        val eventVariables = buildMap {
            put("eventId", eventId.toString())
            put("userId", event.userId.toString())
            put("eventName", event.name)
            event.properties.value.forEach {
                put("event.${it.key}", it.value)
            }
        }

        val stepVariables = if (variablesJson.isNullOrBlank()) {
            emptyMap()
        } else {
            runCatching {
                objectMapper.readValue(variablesJson, object : TypeReference<Map<String, String>>() {})
            }.getOrElse {
                emptyMap()
            }
        }

        return eventVariables + stepVariables
    }
}
