package com.manage.crm.segment.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.segment.domain.SegmentCondition
import com.manage.crm.segment.domain.SegmentOperator
import com.manage.crm.segment.domain.SegmentValueType
import com.manage.crm.segment.domain.repository.SegmentConditionRepository
import com.manage.crm.segment.domain.repository.SegmentRepository
import com.manage.crm.support.exception.NotFoundByIdException
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class SegmentTargetUser(
    val id: Long,
    val userAttributesJson: String,
    val createdAt: LocalDateTime?,
)

data class SegmentTargetEvent(
    val userId: Long,
    val name: String,
    val occurredAt: LocalDateTime?,
)

data class SegmentTargetRuleSet(
    val conditions: List<SegmentCondition>,
    val requiresEventCondition: Boolean,
)

@Service
class SegmentTargetingService(
    private val segmentRepository: SegmentRepository,
    private val segmentConditionRepository: SegmentConditionRepository,
    private val objectMapper: ObjectMapper,
) {
    /**
     * Loads and validates a segment rule set.
     */
    suspend fun loadRuleSet(segmentId: Long): SegmentTargetRuleSet? {
        val segment = segmentRepository.findById(segmentId) ?: throw NotFoundByIdException("Segment", segmentId)
        if (!segment.active) {
            return null
        }

        val conditions = segmentConditionRepository.findBySegmentIdOrderByPositionAsc(segmentId).toList()
        if (conditions.isEmpty()) {
            return null
        }

        return SegmentTargetRuleSet(
            conditions = conditions,
            requiresEventCondition = conditions.any { it.fieldName.startsWith("event.") },
        )
    }

    /**
     * Evaluates all loaded conditions and returns matched user ids.
     */
    fun resolveUserIds(
        ruleSet: SegmentTargetRuleSet,
        users: List<SegmentTargetUser>,
        eventsByUserId: Map<Long, List<SegmentTargetEvent>>,
    ): List<Long> {
        if (users.isEmpty()) {
            return emptyList()
        }

        val conditionMatchesEvents = ruleSet.requiresEventCondition && eventsByUserId.isEmpty()
        if (conditionMatchesEvents) {
            return emptyList()
        }

        return users
            .filter { user ->
                val userAttributes = parseUserAttributes(user)
                val userEvents = eventsByUserId[user.id].orEmpty()
                ruleSet.conditions.all { condition ->
                    matchesCondition(
                        user = user,
                        userAttributes = userAttributes,
                        userEvents = userEvents,
                        condition = condition,
                    )
                }
            }.map { it.id }
    }

    /**
     * Parses user attribute json into a tree for dynamic field lookup.
     */
    private fun parseUserAttributes(user: SegmentTargetUser): JsonNode? =
        runCatching { objectMapper.readTree(user.userAttributesJson) }.getOrNull()

    /**
     * Dispatches one condition evaluation by field name.
     */
    private fun matchesCondition(
        user: SegmentTargetUser,
        userAttributes: JsonNode?,
        userEvents: List<SegmentTargetEvent>,
        condition: SegmentCondition,
    ): Boolean {
        val valueNode = runCatching { objectMapper.readTree(condition.conditionValue) }.getOrNull() ?: return false
        return when (condition.fieldName) {
            "user.id" -> matchSingle(user.id, condition, valueNode)
            "user.email" -> matchSingle(userAttributes?.get("email")?.asText(), condition, valueNode)
            "user.name" -> matchSingle(userAttributes?.get("name")?.asText(), condition, valueNode)
            "user.createdAt" -> matchSingle(user.createdAt, condition, valueNode)
            "event.name" -> matchMany(userEvents.map { it.name }, condition, valueNode)
            "event.occurredAt" -> matchMany(userEvents.mapNotNull { it.occurredAt }, condition, valueNode)
            else -> false
        }
    }

    /**
     * Evaluates an event-field condition against a list of actual values.
     *
     * `NEQ` requires all values to satisfy inequality, while other operators match on any value.
     */
    private fun matchMany(
        actualValues: List<Any>,
        condition: SegmentCondition,
        expectedValue: JsonNode,
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

    /**
     * Evaluates a single actual value using parsed operator/valueType.
     */
    private fun matchSingle(
        actualValue: Any?,
        condition: SegmentCondition,
        expectedValue: JsonNode,
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

    /**
     * Matches string values for string-compatible operators.
     */
    private fun matchString(
        actual: String,
        operator: SegmentOperator,
        expectedValue: JsonNode,
    ): Boolean =
        when (operator) {
            SegmentOperator.EQ -> actual == expectedValue.asText()
            SegmentOperator.NEQ -> actual != expectedValue.asText()
            SegmentOperator.CONTAINS -> actual.contains(expectedValue.asText())
            SegmentOperator.IN -> expectedValue.any { actual == it.asText() }
            else -> false
        }

    /**
     * Matches numeric values for number operators including `IN` and `BETWEEN`.
     */
    private fun matchNumber(
        actual: BigDecimal,
        operator: SegmentOperator,
        expectedValue: JsonNode,
    ): Boolean {
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

    /**
     * Matches date-time values using ISO-8601 parsing with offset/local fallback.
     */
    private fun matchDateTime(
        actual: LocalDateTime,
        operator: SegmentOperator,
        expectedValue: JsonNode,
    ): Boolean {
        fun parseOrNull(value: String): LocalDateTime? =
            runCatching {
                OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime()
            }.recoverCatching {
                LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)
            }.getOrNull()

        return when (operator) {
            SegmentOperator.EQ -> parseOrNull(expectedValue.asText())?.let { actual == it } ?: false
            SegmentOperator.GT -> parseOrNull(expectedValue.asText())?.let { actual > it } ?: false
            SegmentOperator.GTE -> parseOrNull(expectedValue.asText())?.let { actual >= it } ?: false
            SegmentOperator.LT -> parseOrNull(expectedValue.asText())?.let { actual < it } ?: false
            SegmentOperator.LTE -> parseOrNull(expectedValue.asText())?.let { actual <= it } ?: false
            SegmentOperator.BETWEEN -> {
                val start = expectedValue[0]?.asText()?.let { parseOrNull(it) } ?: return false
                val end = expectedValue[1]?.asText()?.let { parseOrNull(it) } ?: return false
                actual in start..end
            }
            else -> false
        }
    }

    /**
     * Matches boolean values for EQ/NEQ operators.
     */
    private fun matchBoolean(
        actual: Boolean,
        operator: SegmentOperator,
        expectedValue: JsonNode,
    ): Boolean {
        val expected = expectedValue.asBoolean()
        return when (operator) {
            SegmentOperator.EQ -> actual == expected
            SegmentOperator.NEQ -> actual != expected
            else -> false
        }
    }

    /**
     * Converts runtime value to [BigDecimal] for number evaluation.
     */
    private fun toBigDecimal(value: Any): BigDecimal? =
        when (value) {
            is BigDecimal -> value
            is Number -> value.toString().toBigDecimalOrNull()
            is String -> value.toBigDecimalOrNull()
            else -> null
        }

    /**
     * Converts runtime value to [LocalDateTime] using ISO-8601 offset/local parsing.
     */
    private fun toLocalDateTime(value: Any): LocalDateTime? =
        when (value) {
            is LocalDateTime -> value
            is String ->
                runCatching { OffsetDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime() }
                    .recoverCatching { LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME) }
                    .getOrNull()
            else -> null
        }

    /**
     * Converts runtime value to strict boolean.
     */
    private fun toBoolean(value: Any): Boolean? =
        when (value) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull()
            else -> null
        }
}
