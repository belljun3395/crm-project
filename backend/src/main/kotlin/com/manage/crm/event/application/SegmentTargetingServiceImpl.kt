package com.manage.crm.event.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.segment.application.SegmentOperator
import com.manage.crm.segment.application.SegmentValueType
import com.manage.crm.segment.domain.SegmentCondition
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Service
class SegmentTargetingServiceImpl(
    private val segmentRepository: SegmentRepository,
    private val segmentConditionRepository: SegmentConditionRepository,
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
    private val objectMapper: ObjectMapper
) : SegmentTargetingService {

    override suspend fun resolveUserIds(segmentId: Long): List<Long> {
        val segment = segmentRepository.findById(segmentId) ?: throw NotFoundByIdException("Segment", segmentId)
        if (!segment.active) {
            return emptyList()
        }

        val conditions = segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId).toList()
        if (conditions.isEmpty()) {
            return emptyList()
        }

        val users = userRepository.findAll().toList()
        if (users.isEmpty()) {
            return emptyList()
        }

        val userIds = users.mapNotNull { it.id }
        val requiresEventCondition = conditions.any { it.fieldName.startsWith("event.") }
        val eventsByUserId = if (requiresEventCondition && userIds.isNotEmpty()) {
            eventRepository.findAllByUserIdIn(userIds).groupBy { it.userId }
        } else {
            emptyMap()
        }

        return users
            .filter { user ->
                val userId = user.id ?: return@filter false
                val userAttributes = parseUserAttributes(user)
                val userEvents = eventsByUserId[userId].orEmpty()
                conditions.all { condition ->
                    matchesCondition(
                        user = user,
                        userAttributes = userAttributes,
                        userEvents = userEvents,
                        condition = condition
                    )
                }
            }
            .mapNotNull { it.id }
    }

    private fun parseUserAttributes(user: User): JsonNode? {
        return runCatching { objectMapper.readTree(user.userAttributes.value) }.getOrNull()
    }

    private fun matchesCondition(
        user: User,
        userAttributes: JsonNode?,
        userEvents: List<Event>,
        condition: SegmentCondition
    ): Boolean {
        val valueNode = runCatching { objectMapper.readTree(condition.conditionValue) }.getOrNull() ?: return false
        return when (condition.fieldName) {
            "user.id" -> matchSingle(user.id, condition, valueNode)
            "user.email" -> matchSingle(userAttributes?.get("email")?.asText(), condition, valueNode)
            "user.name" -> matchSingle(userAttributes?.get("name")?.asText(), condition, valueNode)
            "user.createdAt" -> matchSingle(user.createdAt, condition, valueNode)
            "event.name" -> matchMany(userEvents.map { it.name }, condition, valueNode)
            "event.occurredAt" -> matchMany(userEvents.mapNotNull { it.createdAt }, condition, valueNode)
            else -> false
        }
    }

    private fun matchMany(
        actualValues: List<Any>,
        condition: SegmentCondition,
        expectedValue: JsonNode
    ): Boolean {
        if (actualValues.isEmpty()) {
            return false
        }
        return if (condition.operator.equals(SegmentOperator.NEQ.name, ignoreCase = true)) {
            actualValues.all { actual -> matchSingle(actual, condition, expectedValue) }
        } else {
            actualValues.any { actual -> matchSingle(actual, condition, expectedValue) }
        }
    }

    private fun matchSingle(
        actualValue: Any?,
        condition: SegmentCondition,
        expectedValue: JsonNode
    ): Boolean {
        if (actualValue == null) {
            return false
        }

        val operator = SegmentOperator.from(condition.operator)
        val valueType = SegmentValueType.from(condition.valueType)

        return when (valueType) {
            SegmentValueType.STRING -> matchString(actualValue.toString(), operator, expectedValue)
            SegmentValueType.NUMBER -> toBigDecimal(actualValue)?.let { matchNumber(it, operator, expectedValue) } ?: false
            SegmentValueType.DATETIME -> toLocalDateTime(actualValue)?.let { matchDateTime(it, operator, expectedValue) } ?: false
            SegmentValueType.BOOLEAN -> toBoolean(actualValue)?.let { matchBoolean(it, operator, expectedValue) } ?: false
        }
    }

    private fun matchString(actual: String, operator: SegmentOperator, expectedValue: JsonNode): Boolean {
        return when (operator) {
            SegmentOperator.EQ -> actual == expectedValue.asText()
            SegmentOperator.NEQ -> actual != expectedValue.asText()
            SegmentOperator.CONTAINS -> actual.contains(expectedValue.asText())
            SegmentOperator.IN -> expectedValue.any { actual == it.asText() }
            else -> false
        }
    }

    private fun matchNumber(actual: BigDecimal, operator: SegmentOperator, expectedValue: JsonNode): Boolean {
        return when (operator) {
            SegmentOperator.EQ -> actual.compareTo(expectedValue.decimalValue()) == 0
            SegmentOperator.NEQ -> actual.compareTo(expectedValue.decimalValue()) != 0
            SegmentOperator.GT -> actual > expectedValue.decimalValue()
            SegmentOperator.GTE -> actual >= expectedValue.decimalValue()
            SegmentOperator.LT -> actual < expectedValue.decimalValue()
            SegmentOperator.LTE -> actual <= expectedValue.decimalValue()
            SegmentOperator.IN -> expectedValue.any { actual.compareTo(it.decimalValue()) == 0 }
            SegmentOperator.BETWEEN -> {
                val start = expectedValue[0]?.decimalValue() ?: return false
                val end = expectedValue[1]?.decimalValue() ?: return false
                actual >= start && actual <= end
            }
            else -> false
        }
    }

    private fun matchDateTime(actual: LocalDateTime, operator: SegmentOperator, expectedValue: JsonNode): Boolean {
        fun parseOrNull(value: String): LocalDateTime? {
            return runCatching {
                OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime()
            }.recoverCatching {
                LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)
            }.getOrNull()
        }

        return when (operator) {
            SegmentOperator.EQ -> parseOrNull(expectedValue.asText())?.let { actual == it } ?: false
            SegmentOperator.GT -> parseOrNull(expectedValue.asText())?.let { actual > it } ?: false
            SegmentOperator.GTE -> parseOrNull(expectedValue.asText())?.let { actual >= it } ?: false
            SegmentOperator.LT -> parseOrNull(expectedValue.asText())?.let { actual < it } ?: false
            SegmentOperator.LTE -> parseOrNull(expectedValue.asText())?.let { actual <= it } ?: false
            SegmentOperator.BETWEEN -> {
                val start = expectedValue[0]?.asText()?.let { parseOrNull(it) } ?: return false
                val end = expectedValue[1]?.asText()?.let { parseOrNull(it) } ?: return false
                actual >= start && actual <= end
            }
            else -> false
        }
    }

    private fun matchBoolean(actual: Boolean, operator: SegmentOperator, expectedValue: JsonNode): Boolean {
        val expected = expectedValue.asBoolean()
        return when (operator) {
            SegmentOperator.EQ -> actual == expected
            SegmentOperator.NEQ -> actual != expected
            else -> false
        }
    }

    private fun toBigDecimal(value: Any): BigDecimal? {
        return when (value) {
            is BigDecimal -> value
            is Number -> value.toString().toBigDecimalOrNull()
            is String -> value.toBigDecimalOrNull()
            else -> null
        }
    }

    private fun toLocalDateTime(value: Any): LocalDateTime? {
        return when (value) {
            is LocalDateTime -> value
            is String -> runCatching { OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime() }
                .recoverCatching { LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME) }
                .getOrNull()
            else -> null
        }
    }

    private fun toBoolean(value: Any): Boolean? {
        return when (value) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull()
            else -> null
        }
    }
}
