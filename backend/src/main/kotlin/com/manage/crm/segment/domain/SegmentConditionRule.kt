package com.manage.crm.segment.domain

import com.manage.crm.segment.exception.InvalidSegmentConditionException

/**
 * Segment condition value type definition shared by validator and targeting service.
 */
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

/**
 * Segment condition operator definition shared by validator and targeting service.
 */
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
