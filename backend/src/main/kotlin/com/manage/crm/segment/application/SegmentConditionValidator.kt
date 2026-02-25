package com.manage.crm.segment.application

import com.fasterxml.jackson.databind.JsonNode
import com.manage.crm.segment.exception.InvalidSegmentConditionException

enum class SegmentValueType {
    STRING,
    NUMBER,
    DATETIME,
    BOOLEAN;

    companion object {
        fun from(value: String): SegmentValueType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw InvalidSegmentConditionException("Unsupported valueType: $value")
        }
    }
}

enum class SegmentOperator {
    EQ,
    NEQ,
    GT,
    GTE,
    LT,
    LTE,
    IN,
    CONTAINS,
    BETWEEN;

    companion object {
        fun from(value: String): SegmentOperator {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw InvalidSegmentConditionException("Unsupported operator: $value")
        }
    }
}

object SegmentConditionValidator {
    private val allowedFields = setOf(
        "user.id",
        "user.email",
        "user.name",
        "user.createdAt",
        "event.name",
        "event.occurredAt"
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
        if (field !in allowedFields) {
            throw InvalidSegmentConditionException("Unsupported field: $field")
        }

        val parsedValueType = SegmentValueType.from(valueType)
        val parsedOperator = SegmentOperator.from(operator)
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
                if (!value.all { it.isTextual }) {
                    throw InvalidSegmentConditionException("DATETIME BETWEEN value must contain only string items")
                }
            }
            else -> {
                if (!value.isTextual) {
                    throw InvalidSegmentConditionException("DATETIME value must be string format")
                }
            }
        }
    }

    private fun validateBooleanValue(value: JsonNode) {
        if (!value.isBoolean) {
            throw InvalidSegmentConditionException("BOOLEAN value must be true/false")
        }
    }
}
