package com.manage.crm.journey.application

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.action.application.ActionChannel
import com.manage.crm.action.application.ActionDispatchIn
import com.manage.crm.action.application.ActionDispatchService
import com.manage.crm.action.application.ActionDispatchStatus
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneyExecution
import com.manage.crm.journey.domain.JourneyExecutionHistory
import com.manage.crm.journey.domain.JourneySegmentCountState
import com.manage.crm.journey.domain.JourneySegmentUserState
import com.manage.crm.journey.domain.JourneyStep
import com.manage.crm.journey.domain.JourneyStepDeduplication
import com.manage.crm.journey.domain.repository.JourneyExecutionHistoryRepository
import com.manage.crm.journey.domain.repository.JourneyExecutionRepository
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneySegmentCountStateRepository
import com.manage.crm.journey.domain.repository.JourneySegmentUserStateRepository
import com.manage.crm.journey.domain.repository.JourneyStepDeduplicationRepository
import com.manage.crm.journey.domain.repository.JourneyStepRepository
import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.security.MessageDigest
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
    private val journeySegmentUserStateRepository: JourneySegmentUserStateRepository,
    private val journeySegmentCountStateRepository: JourneySegmentCountStateRepository,
    private val segmentReadPort: SegmentReadPort,
    private val actionDispatchService: ActionDispatchService,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = KotlinLogging.logger {}

    suspend fun onEvent(event: Event) {
        val eventJourneys = journeyRepository
            .findAllByTriggerTypeAndTriggerEventNameAndActiveTrue(
                triggerType = JourneyTriggerType.EVENT.name,
                triggerEventName = event.name
            )
            .toList()

        val eventId = requireNotNull(event.id) { "Event id cannot be null" }
        eventJourneys.forEach { journey ->
            val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
            val triggerKey = "$journeyId:$eventId:${event.userId}"
            executeJourney(journey, event, triggerKey)
        }

        processConditionTriggeredJourneys(event)
    }

    private suspend fun processConditionTriggeredJourneys(event: Event) {
        val conditionJourneys = journeyRepository
            .findAllByTriggerTypeAndActiveTrue(JourneyTriggerType.CONDITION.name)
            .toList()
        if (conditionJourneys.isEmpty()) {
            return
        }

        val eventId = requireNotNull(event.id) { "Event id cannot be null" }
        conditionJourneys.forEach { journey ->
            runCatching {
                val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
                val conditionExpression = if (!journey.triggerEventName.isNullOrBlank()) {
                    journey.triggerEventName
                } else {
                    val steps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()
                    resolveConditionExpression(journey, steps)
                }

                if (conditionExpression.isNullOrBlank()) {
                    log.warn { "Skip CONDITION journey without condition expression: journeyId=$journeyId" }
                    return@runCatching
                }

                if (!evaluateCondition(conditionExpression, event)) {
                    return@runCatching
                }

                val triggerKey = "$journeyId:CONDITION:$eventId:${event.userId}"
                executeJourney(journey, event, triggerKey)
            }.onFailure { error ->
                val journeyId = journey.id
                log.error(error) { "Failed to process CONDITION journey: journeyId=$journeyId" }
            }
        }
    }

    private fun resolveConditionExpression(journey: Journey, steps: List<JourneyStep>): String? {
        if (!journey.triggerEventName.isNullOrBlank()) {
            return journey.triggerEventName
        }

        return steps.firstOrNull { step ->
            runCatching { JourneyStepType.from(step.stepType) }.getOrNull() == JourneyStepType.BRANCH &&
                !step.conditionExpression.isNullOrBlank()
        }?.conditionExpression
    }

    suspend fun onSegmentContextChanged(changedUserIds: List<Long>? = null) {
        val journeys = journeyRepository.findAllByTriggerTypeAndActiveTrue(JourneyTriggerType.SEGMENT.name).toList()
        if (journeys.isEmpty()) {
            return
        }

        journeys.forEach { journey ->
            runCatching {
                processSegmentJourney(journey, changedUserIds)
            }.onFailure { error ->
                val journeyId = journey.id
                log.error(error) { "Failed to process segment-triggered journey: journeyId=$journeyId" }
            }
        }
    }

    private suspend fun processSegmentJourney(journey: Journey, changedUserIds: List<Long>?) {
        val segmentId = journey.triggerSegmentId ?: return
        val segmentTriggerEventType = JourneySegmentTriggerEventType.from(
            journey.triggerSegmentEvent ?: JourneySegmentTriggerEventType.ENTER.name
        )

        when (segmentTriggerEventType) {
            JourneySegmentTriggerEventType.ENTER,
            JourneySegmentTriggerEventType.EXIT,
            JourneySegmentTriggerEventType.UPDATE -> processSegmentUserTriggerJourney(
                journey = journey,
                segmentId = segmentId,
                segmentTriggerEventType = segmentTriggerEventType,
                changedUserIds = changedUserIds
            )

            JourneySegmentTriggerEventType.COUNT_REACHED,
            JourneySegmentTriggerEventType.COUNT_DROPPED -> processSegmentCountTriggerJourney(
                journey = journey,
                segmentId = segmentId,
                segmentTriggerEventType = segmentTriggerEventType
            )
        }
    }

    private suspend fun processSegmentUserTriggerJourney(
        journey: Journey,
        segmentId: Long,
        segmentTriggerEventType: JourneySegmentTriggerEventType,
        changedUserIds: List<Long>?
    ) {
        val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
        val currentMatchedUserIds = segmentReadPort.findTargetUserIds(segmentId, null).toSet()
        val existingStates = journeySegmentUserStateRepository.findAllByJourneyId(journeyId)
        val existingStateByUserId = existingStates.associateBy { it.userId }

        val candidateUserIds = (existingStateByUserId.keys + currentMatchedUserIds).toMutableSet().apply {
            if (!changedUserIds.isNullOrEmpty()) {
                addAll(changedUserIds)
            }
        }
        if (candidateUserIds.isEmpty()) {
            return
        }

        val watchFields = parseTriggerWatchFields(journey.triggerSegmentWatchFields)
        val usersById = if (segmentTriggerEventType == JourneySegmentTriggerEventType.UPDATE) {
            val inSegmentCandidates = candidateUserIds.filter { currentMatchedUserIds.contains(it) }
            if (inSegmentCandidates.isEmpty()) {
                emptyMap()
            } else {
                userRepository.findAllByIdIn(inSegmentCandidates)
                    .mapNotNull { user -> user.id?.let { it to user } }
                    .toMap()
            }
        } else {
            emptyMap()
        }

        candidateUserIds.sorted().forEach { userId ->
            val previousState = existingStateByUserId[userId]
            val wasInSegment = previousState?.inSegment ?: false
            val isInSegment = currentMatchedUserIds.contains(userId)

            val currentWatchHash = if (segmentTriggerEventType == JourneySegmentTriggerEventType.UPDATE && isInSegment) {
                usersById[userId]?.let { user -> calculateWatchFieldHash(user, watchFields) }
            } else {
                null
            }
            val previousWatchHash = previousState?.attributesHash

            var transitionVersion = previousState?.transitionVersion ?: 0L
            val shouldTrigger = when (segmentTriggerEventType) {
                JourneySegmentTriggerEventType.ENTER -> !wasInSegment && isInSegment
                JourneySegmentTriggerEventType.EXIT -> wasInSegment && !isInSegment
                JourneySegmentTriggerEventType.UPDATE -> {
                    wasInSegment && isInSegment &&
                        previousWatchHash != null &&
                        currentWatchHash != null &&
                        previousWatchHash != currentWatchHash
                }
                else -> false
            }

            if (shouldTrigger) {
                transitionVersion += 1L
            }

            val nextWatchHash = if (segmentTriggerEventType == JourneySegmentTriggerEventType.UPDATE && isInSegment) {
                currentWatchHash
            } else {
                null
            }

            saveJourneySegmentUserState(
                previousState = previousState,
                journeyId = journeyId,
                userId = userId,
                isInSegment = isInSegment,
                attributesHash = nextWatchHash,
                transitionVersion = transitionVersion
            )

            if (!shouldTrigger) {
                return@forEach
            }

            val triggerKey = "$journeyId:SEGMENT:${segmentTriggerEventType.name}:$userId:$transitionVersion"
            val syntheticEvent = buildSegmentSyntheticEvent(
                userId = userId,
                segmentId = segmentId,
                segmentTriggerEventType = segmentTriggerEventType,
                segmentCount = currentMatchedUserIds.size.toLong(),
                transitionVersion = transitionVersion,
                threshold = journey.triggerSegmentCountThreshold
            )
            executeJourney(journey, syntheticEvent, triggerKey)
        }
    }

    private suspend fun processSegmentCountTriggerJourney(
        journey: Journey,
        segmentId: Long,
        segmentTriggerEventType: JourneySegmentTriggerEventType
    ) {
        val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
        val threshold = journey.triggerSegmentCountThreshold ?: return
        if (threshold <= 0L) {
            return
        }

        val currentCount = segmentReadPort.findTargetUserIds(segmentId, null).size.toLong()
        val previousState = journeySegmentCountStateRepository.findByJourneyId(journeyId)
        val previousCount = previousState?.lastCount ?: 0L
        val previousVersion = previousState?.transitionVersion ?: 0L

        val crossed = when (segmentTriggerEventType) {
            JourneySegmentTriggerEventType.COUNT_REACHED -> previousCount < threshold && currentCount >= threshold
            JourneySegmentTriggerEventType.COUNT_DROPPED -> previousCount >= threshold && currentCount < threshold
            else -> false
        }

        val transitionVersion = if (crossed) previousVersion + 1L else previousVersion
        saveJourneySegmentCountState(previousState, journeyId, currentCount, transitionVersion)

        if (!crossed) {
            return
        }

        val triggerKey = "$journeyId:SEGMENT:${segmentTriggerEventType.name}:$threshold:$transitionVersion"
        val syntheticEvent = buildSegmentSyntheticEvent(
            userId = 0L,
            segmentId = segmentId,
            segmentTriggerEventType = segmentTriggerEventType,
            segmentCount = currentCount,
            transitionVersion = transitionVersion,
            threshold = threshold
        )
        executeJourney(journey, syntheticEvent, triggerKey)
    }

    private suspend fun executeJourney(journey: Journey, event: Event, triggerKey: String) {
        val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
        val eventId = requireNotNull(event.id) { "Event id cannot be null" }

        if (journeyExecutionRepository.findByTriggerKey(triggerKey) != null) {
            log.debug { "Skip duplicated journey execution by triggerKey=$triggerKey" }
            return
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

    private suspend fun executeJourneySteps(execution: JourneyExecution, event: Event) {
        val journeyId = execution.journeyId
        val executionId = requireNotNull(execution.id) { "JourneyExecution id cannot be null" }

        val steps = journeyStepRepository.findAllByJourneyIdOrderByStepOrderAsc(journeyId).toList()

        for (step in steps) {
            execution.currentStepOrder = step.stepOrder
            journeyExecutionRepository.save(execution)

            val decision = when (JourneyStepType.from(step.stepType)) {
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

        val dedupeKey = "$executionId:$stepId"
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

    private suspend fun resolveStepVariables(variablesJson: String?, event: Event): Map<String, String> {
        val eventId = requireNotNull(event.id) { "Event id cannot be null" }
        val eventVariables = buildMap {
            put("eventId", eventId.toString())
            put("userId", event.userId.toString())
            put("eventName", event.name)
            event.properties.value.forEach {
                put("event.${it.key}", it.value)
            }
        }
        val userVariables = userRepository.findById(event.userId)
            ?.let { user ->
                buildMap {
                    put("user.id", event.userId.toString())
                    put("user.externalId", user.externalId)
                    runCatching {
                        user.userAttributes.getValue(RequiredUserAttributeKey.EMAIL, objectMapper)
                    }.getOrNull()?.let { put("user.email", it) }
                    runCatching {
                        user.userAttributes.getValue(RequiredUserAttributeKey.NAME, objectMapper)
                    }.getOrNull()?.let { put("user.name", it) }
                }
            }
            ?: mapOf("user.id" to event.userId.toString())

        val stepVariables = if (variablesJson.isNullOrBlank()) {
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

    private suspend fun saveJourneySegmentUserState(
        previousState: JourneySegmentUserState?,
        journeyId: Long,
        userId: Long,
        isInSegment: Boolean,
        attributesHash: String?,
        transitionVersion: Long
    ) {
        if (previousState == null) {
            if (!isInSegment && attributesHash == null && transitionVersion == 0L) {
                return
            }
            journeySegmentUserStateRepository.save(
                JourneySegmentUserState.new(
                    journeyId = journeyId,
                    userId = userId,
                    inSegment = isInSegment,
                    attributesHash = attributesHash,
                    transitionVersion = transitionVersion
                )
            )
            return
        }

        previousState.inSegment = isInSegment
        previousState.attributesHash = attributesHash
        previousState.transitionVersion = transitionVersion
        journeySegmentUserStateRepository.save(previousState)
    }

    private suspend fun saveJourneySegmentCountState(
        previousState: JourneySegmentCountState?,
        journeyId: Long,
        currentCount: Long,
        transitionVersion: Long
    ) {
        if (previousState == null) {
            journeySegmentCountStateRepository.save(
                JourneySegmentCountState.new(
                    journeyId = journeyId,
                    lastCount = currentCount,
                    transitionVersion = transitionVersion
                )
            )
            return
        }

        previousState.lastCount = currentCount
        previousState.transitionVersion = transitionVersion
        journeySegmentCountStateRepository.save(previousState)
    }

    private fun parseTriggerWatchFields(raw: String?): List<String> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }
        return runCatching {
            objectMapper.readValue(raw, object : TypeReference<List<String>>() {})
        }.getOrElse {
            emptyList()
        }
    }

    private fun calculateWatchFieldHash(user: User, watchFields: List<String>): String {
        if (watchFields.isEmpty()) {
            return ""
        }

        val userAttributes = runCatching { objectMapper.readTree(user.userAttributes.value) }.getOrNull()
        val serialized = watchFields.joinToString("|") { field ->
            val value = resolveWatchFieldValue(field, user, userAttributes)
            "$field=${value ?: ""}"
        }
        return sha256(serialized)
    }

    private fun resolveWatchFieldValue(field: String, user: User, userAttributes: JsonNode?): String? {
        return when (field) {
            "user.id" -> user.id?.toString()
            "user.externalId" -> user.externalId
            "user.createdAt" -> user.createdAt?.toString()
            "user.updatedAt" -> user.updatedAt?.toString()
            else -> {
                if (!field.startsWith("user.")) {
                    null
                } else {
                    val keyPath = field.removePrefix("user.")
                    resolveJsonPathValue(userAttributes, keyPath)
                }
            }
        }
    }

    private fun resolveJsonPathValue(root: JsonNode?, keyPath: String): String? {
        if (root == null || keyPath.isBlank()) {
            return null
        }
        val finalNode = keyPath.split(".").fold(root as JsonNode?) { node, key ->
            node?.get(key)
        } ?: return null

        return if (finalNode.isValueNode) finalNode.asText() else finalNode.toString()
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun buildSegmentSyntheticEvent(
        userId: Long,
        segmentId: Long,
        segmentTriggerEventType: JourneySegmentTriggerEventType,
        segmentCount: Long,
        transitionVersion: Long,
        threshold: Long?
    ): Event {
        val properties = mutableListOf(
            EventProperty("segmentId", segmentId.toString()),
            EventProperty("segmentTriggerEvent", segmentTriggerEventType.name),
            EventProperty("segmentCount", segmentCount.toString()),
            EventProperty("transitionVersion", transitionVersion.toString())
        )
        if (threshold != null) {
            properties.add(EventProperty("segmentThreshold", threshold.toString()))
        }

        val syntheticEventId = transitionVersion.coerceAtLeast(1L)
        return Event.new(
            id = syntheticEventId,
            name = "SEGMENT_${segmentTriggerEventType.name}",
            userId = userId,
            properties = EventProperties(properties),
            createdAt = LocalDateTime.now()
        )
    }
}
