package com.manage.crm.segment.util

import com.fasterxml.jackson.databind.JsonNode
import com.manage.crm.segment.domain.SegmentOperator
import com.manage.crm.segment.domain.SegmentValueType
import com.manage.crm.segment.exception.InvalidSegmentConditionException
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object SegmentConditionValidator {
    private val fieldValueTypeMap = mapOf(
        "user.id" to SegmentValueType.NUMBER,
        "user.email" to SegmentValueType.STRING,
        "user.name" to SegmentValueType.STRING,
        "user.createdAt" to SegmentValueType.DATETIME,
        "event.name" to SegmentValueType.STRING,
        "event.occurredAt" to SegmentValueType.DATETIME
    )

    private val allowedOperatorsByType = mapOf(
        SegmentValueType.STRING to setOf(
            SegmentOperator.EQ,
            SegmentOperator.NEQ,
            SegmentOperator.CONTAINS,
            SegmentOperator.IN
        ),
        SegmentValueType.NUMBER to setOf(
            SegmentOperator.EQ,
            SegmentOperator.NEQ,
            SegmentOperator.GT,
            SegmentOperator.GTE,
            SegmentOperator.LT,
            SegmentOperator.LTE,
            SegmentOperator.IN,
            SegmentOperator.BETWEEN
        ),
        SegmentValueType.DATETIME to setOf(
            SegmentOperator.EQ,
            SegmentOperator.GT,
            SegmentOperator.GTE,
            SegmentOperator.LT,
            SegmentOperator.LTE,
            SegmentOperator.BETWEEN
        ),
        SegmentValueType.BOOLEAN to setOf(
            SegmentOperator.EQ,
            SegmentOperator.NEQ
        )
    )

    fun validate(
        field: String,
        operator: String,
        valueType: String,
        value: JsonNode
    ) {
        if (field.isBlank()) {
            throw InvalidSegmentConditionException("field is required")
        }
        if (field !in fieldValueTypeMap.keys) {
            throw InvalidSegmentConditionException("Unsupported field: $field")
        }

        val parsedValueType = SegmentValueType.Companion.from(valueType)
        val requiredValueType = fieldValueTypeMap[field]
            ?: throw InvalidSegmentConditionException("Unsupported field: $field")
        if (parsedValueType != requiredValueType) {
            throw InvalidSegmentConditionException(
                "Field $field requires valueType ${requiredValueType.name}"
            )
        }

        val parsedOperator = SegmentOperator.Companion.from(operator)
        val allowedOperators = allowedOperatorsByType[parsedValueType].orEmpty()

        if (parsedOperator !in allowedOperators) {
            throw InvalidSegmentConditionException(
                "Operator $operator is not allowed for valueType $valueType"
            )
        }

        validateValue(parsedValueType, parsedOperator, value)
    }

    private fun validateValue(
        valueType: SegmentValueType,
        operator: SegmentOperator,
        value: JsonNode
    ) {
        if (value.isNull || value.isMissingNode) {
            throw InvalidSegmentConditionException("value is required")
        }

        when (operator) {
            SegmentOperator.IN -> {
                if (!value.isArray || value.isEmpty) {
                    throw InvalidSegmentConditionException("IN operator requires non-empty array value")
                }
            }
            SegmentOperator.BETWEEN -> {
                if (!value.isArray || value.size() != 2) {
                    throw InvalidSegmentConditionException("BETWEEN operator requires array value with exactly two items")
                }
            }
            else -> {
                if (value.isArray) {
                    throw InvalidSegmentConditionException("$operator operator does not allow array value")
                }
            }
        }

        when (valueType) {
            SegmentValueType.STRING -> validateStringValue(operator, value)
            SegmentValueType.NUMBER -> validateNumberValue(operator, value)
            SegmentValueType.DATETIME -> validateDateTimeValue(operator, value)
            SegmentValueType.BOOLEAN -> validateBooleanValue(value)
        }
    }

    private fun validateStringValue(operator: SegmentOperator, value: JsonNode) {
        when (operator) {
            SegmentOperator.IN -> {
                if (!value.all { it.isTextual }) {
                    throw InvalidSegmentConditionException("STRING IN value must contain only string items")
                }
            }
            else -> {
                if (!value.isTextual) {
                    throw InvalidSegmentConditionException("STRING value must be a string")
                }
            }
        }
    }

    private fun validateNumberValue(operator: SegmentOperator, value: JsonNode) {
        when (operator) {
            SegmentOperator.IN, SegmentOperator.BETWEEN -> {
                if (!value.all { it.isNumber }) {
                    throw InvalidSegmentConditionException("NUMBER $operator value must contain only numeric items")
                }
            }
            else -> {
                if (!value.isNumber) {
                    throw InvalidSegmentConditionException("NUMBER value must be numeric")
                }
            }
        }
    }

    private fun validateDateTimeValue(operator: SegmentOperator, value: JsonNode) {
        when (operator) {
            SegmentOperator.BETWEEN -> {
                if (!value.all { it.isTextual && isIsoDateTime(it.asText()) }) {
                    throw InvalidSegmentConditionException("DATETIME BETWEEN value must contain only ISO-8601 string items")
                }
            }
            else -> {
                if (!value.isTextual || !isIsoDateTime(value.asText())) {
                    throw InvalidSegmentConditionException("DATETIME value must be ISO-8601 string format")
                }
            }
        }
    }

    private fun isIsoDateTime(value: String): Boolean {
        return try {
            DateTimeFormatter.ISO_DATE_TIME.parse(value)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }

    private fun validateBooleanValue(value: JsonNode) {
        if (!value.isBoolean) {
            throw InvalidSegmentConditionException("BOOLEAN value must be true/false")
        }
    }
}
