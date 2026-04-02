package com.manage.crm.journey.application.automation.segment

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.event.application.port.query.EventReadPort
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import com.manage.crm.journey.application.dto.JourneySegmentTriggerEventType
import com.manage.crm.journey.application.dto.JourneyTriggerType
import com.manage.crm.journey.domain.Journey
import com.manage.crm.journey.domain.JourneySegmentCountState
import com.manage.crm.journey.domain.JourneySegmentUserState
import com.manage.crm.journey.domain.repository.JourneyRepository
import com.manage.crm.journey.domain.repository.JourneySegmentCountStateRepository
import com.manage.crm.journey.domain.repository.JourneySegmentUserStateRepository
import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.segment.application.port.query.SegmentTargetEventReadModel
import com.manage.crm.segment.application.port.query.SegmentTargetUserReadModel
import com.manage.crm.user.application.port.query.UserReadModel
import com.manage.crm.user.application.port.query.UserReadPort
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.toList
import java.security.MessageDigest
import java.time.LocalDateTime

class SegmentTriggerHandler(
    private val journeyRepository: JourneyRepository,
    private val journeySegmentUserStateRepository: JourneySegmentUserStateRepository,
    private val journeySegmentCountStateRepository: JourneySegmentCountStateRepository,
    private val segmentReadPort: SegmentReadPort,
    private val eventReadPort: EventReadPort,
    private val userReadPort: UserReadPort,
    private val objectMapper: ObjectMapper,
) {
    private val log = KotlinLogging.logger {}

    suspend fun processSegmentTriggeredJourneys(
        changedUserIds: List<Long>? = null,
        executeJourney: suspend (journey: Journey, event: Event, triggerKey: String) -> Unit,
    ) {
        val journeys = journeyRepository.findAllByTriggerTypeAndActiveTrue(JourneyTriggerType.SEGMENT.name).toList()
        if (journeys.isEmpty()) {
            return
        }

        journeys.forEach { journey ->
            runCatching {
                processSegmentJourney(journey, changedUserIds, executeJourney)
            }.onFailure { error ->
                val journeyId = journey.id
                log.error(error) { "Failed to process segment-triggered journey: journeyId=$journeyId" }
            }
        }
    }

    private suspend fun processSegmentJourney(
        journey: Journey,
        changedUserIds: List<Long>?,
        executeJourney: suspend (journey: Journey, event: Event, triggerKey: String) -> Unit,
    ) {
        val segmentId = journey.triggerSegmentId ?: return
        val segmentTriggerEventType =
            JourneySegmentTriggerEventType.from(
                journey.triggerSegmentEvent ?: JourneySegmentTriggerEventType.ENTER.name,
            )

        when (segmentTriggerEventType) {
            JourneySegmentTriggerEventType.ENTER,
            JourneySegmentTriggerEventType.EXIT,
            JourneySegmentTriggerEventType.UPDATE,
            ->
                processSegmentUserTriggerJourney(
                    journey = journey,
                    segmentId = segmentId,
                    segmentTriggerEventType = segmentTriggerEventType,
                    changedUserIds = changedUserIds,
                    executeJourney = executeJourney,
                )

            JourneySegmentTriggerEventType.COUNT_REACHED,
            JourneySegmentTriggerEventType.COUNT_DROPPED,
            ->
                processSegmentCountTriggerJourney(
                    journey = journey,
                    segmentId = segmentId,
                    segmentTriggerEventType = segmentTriggerEventType,
                    executeJourney = executeJourney,
                )
        }
    }

    private suspend fun processSegmentUserTriggerJourney(
        journey: Journey,
        segmentId: Long,
        segmentTriggerEventType: JourneySegmentTriggerEventType,
        changedUserIds: List<Long>?,
        executeJourney: suspend (journey: Journey, event: Event, triggerKey: String) -> Unit,
    ) {
        val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
        val currentMatchedUserIds = resolveSegmentTargetUserIds(segmentId).toSet()
        val existingStates = journeySegmentUserStateRepository.findAllByJourneyId(journeyId)
        val existingStateByUserId = existingStates.associateBy { it.userId }

        val candidateUserIds =
            (existingStateByUserId.keys + currentMatchedUserIds).toMutableSet().apply {
                if (!changedUserIds.isNullOrEmpty()) {
                    addAll(changedUserIds)
                }
            }
        if (candidateUserIds.isEmpty()) {
            return
        }

        val watchFields = parseTriggerWatchFields(journey.triggerSegmentWatchFields)
        val usersById =
            if (segmentTriggerEventType == JourneySegmentTriggerEventType.UPDATE) {
                val inSegmentCandidates = candidateUserIds.filter { currentMatchedUserIds.contains(it) }
                if (inSegmentCandidates.isEmpty()) {
                    emptyMap()
                } else {
                    userReadPort
                        .findAllByIdIn(inSegmentCandidates)
                        .associateBy { it.id }
                }
            } else {
                emptyMap()
            }

        candidateUserIds.sorted().forEach { userId ->
            val previousState = existingStateByUserId[userId]
            val wasInSegment = previousState?.inSegment ?: false
            val isInSegment = currentMatchedUserIds.contains(userId)

            val currentWatchHash =
                if (segmentTriggerEventType == JourneySegmentTriggerEventType.UPDATE && isInSegment) {
                    usersById[userId]?.let { user -> calculateWatchFieldHash(user, watchFields) }
                } else {
                    null
                }
            val previousWatchHash = previousState?.attributesHash

            var transitionVersion = previousState?.transitionVersion ?: 0L
            val shouldTrigger =
                when (segmentTriggerEventType) {
                    JourneySegmentTriggerEventType.ENTER -> !wasInSegment && isInSegment
                    JourneySegmentTriggerEventType.EXIT -> wasInSegment && !isInSegment
                    JourneySegmentTriggerEventType.UPDATE -> {
                        wasInSegment &&
                            isInSegment &&
                            previousWatchHash != null &&
                            currentWatchHash != null &&
                            previousWatchHash != currentWatchHash
                    }
                    else -> false
                }

            if (shouldTrigger) {
                transitionVersion += 1L
            }

            val nextWatchHash =
                if (segmentTriggerEventType == JourneySegmentTriggerEventType.UPDATE && isInSegment) {
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
                transitionVersion = transitionVersion,
            )

            if (!shouldTrigger) {
                return@forEach
            }

            val triggerKey = "$journeyId:SEGMENT:${segmentTriggerEventType.name}:$userId:$transitionVersion"
            val syntheticEvent =
                buildSegmentSyntheticEvent(
                    userId = userId,
                    segmentId = segmentId,
                    segmentTriggerEventType = segmentTriggerEventType,
                    segmentCount = currentMatchedUserIds.size.toLong(),
                    transitionVersion = transitionVersion,
                    threshold = journey.triggerSegmentCountThreshold,
                )
            executeJourney(journey, syntheticEvent, triggerKey)
        }
    }

    private suspend fun processSegmentCountTriggerJourney(
        journey: Journey,
        segmentId: Long,
        segmentTriggerEventType: JourneySegmentTriggerEventType,
        executeJourney: suspend (journey: Journey, event: Event, triggerKey: String) -> Unit,
    ) {
        val journeyId = requireNotNull(journey.id) { "Journey id cannot be null" }
        val threshold = journey.triggerSegmentCountThreshold ?: return
        if (threshold <= 0L) {
            return
        }

        val currentCount = resolveSegmentTargetUserIds(segmentId).size.toLong()
        val previousState = journeySegmentCountStateRepository.findByJourneyId(journeyId)
        val previousCount = previousState?.lastCount ?: 0L
        val previousVersion = previousState?.transitionVersion ?: 0L

        val crossed =
            when (segmentTriggerEventType) {
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
        val syntheticEvent =
            buildSegmentSyntheticEvent(
                userId = 0L,
                segmentId = segmentId,
                segmentTriggerEventType = segmentTriggerEventType,
                segmentCount = currentCount,
                transitionVersion = transitionVersion,
                threshold = threshold,
            )
        executeJourney(journey, syntheticEvent, triggerKey)
    }

    private suspend fun resolveSegmentTargetUserIds(segmentId: Long): List<Long> {
        val users =
            userReadPort
                .findAll()
                .map { user ->
                    SegmentTargetUserReadModel(
                        id = user.id,
                        userAttributesJson = user.userAttributesJson,
                        createdAt = user.createdAt,
                    )
                }
        if (users.isEmpty()) {
            return emptyList()
        }

        val eventsByUserId =
            eventReadPort
                .findAllByUserIdIn(users.map { it.id })
                .groupBy { event -> event.userId }
                .mapValues { (_, events) ->
                    events.map { event ->
                        SegmentTargetEventReadModel(
                            userId = event.userId,
                            name = event.name,
                            occurredAt = event.createdAt,
                        )
                    }
                }

        return segmentReadPort.findTargetUserIds(
            segmentId = segmentId,
            users = users,
            eventsByUserId = eventsByUserId,
        )
    }

    private suspend fun saveJourneySegmentUserState(
        previousState: JourneySegmentUserState?,
        journeyId: Long,
        userId: Long,
        isInSegment: Boolean,
        attributesHash: String?,
        transitionVersion: Long,
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
                    transitionVersion = transitionVersion,
                ),
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
        transitionVersion: Long,
    ) {
        if (previousState == null) {
            journeySegmentCountStateRepository.save(
                JourneySegmentCountState.new(
                    journeyId = journeyId,
                    lastCount = currentCount,
                    transitionVersion = transitionVersion,
                ),
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

    private fun calculateWatchFieldHash(
        user: UserReadModel,
        watchFields: List<String>,
    ): String {
        if (watchFields.isEmpty()) {
            return ""
        }

        val userAttributes = runCatching { objectMapper.readTree(user.userAttributesJson) }.getOrNull()
        val serialized =
            watchFields.joinToString("|") { field ->
                val value = resolveWatchFieldValue(field, user, userAttributes)
                "$field=${value ?: ""}"
            }
        return sha256(serialized)
    }

    private fun resolveWatchFieldValue(
        field: String,
        user: UserReadModel,
        userAttributes: JsonNode?,
    ): String? =
        when (field) {
            "user.id" -> user.id.toString()
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

    private fun resolveJsonPathValue(
        root: JsonNode?,
        keyPath: String,
    ): String? {
        if (root == null || keyPath.isBlank()) {
            return null
        }
        val finalNode =
            keyPath.split(".").fold(root as JsonNode?) { node, key ->
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
        threshold: Long?,
    ): Event {
        val properties =
            mutableListOf(
                EventProperty("segmentId", segmentId.toString()),
                EventProperty("segmentTriggerEvent", segmentTriggerEventType.name),
                EventProperty("segmentCount", segmentCount.toString()),
                EventProperty("transitionVersion", transitionVersion.toString()),
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
            createdAt = LocalDateTime.now(),
        )
    }
}
