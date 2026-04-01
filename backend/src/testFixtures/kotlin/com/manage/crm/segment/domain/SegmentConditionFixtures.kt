package com.manage.crm.segment.domain

import java.time.LocalDateTime
import kotlin.random.Random

class SegmentConditionFixtures private constructor() {
    private var id: Long? = null
    private var segmentId: Long = -1L
    private var fieldName: String = "user.email"
    private var operator: String = "EQ"
    private var valueType: String = "STRING"
    private var conditionValue: String = "\"test@example.com\""
    private var position: Int = 1
    private var createdAt: LocalDateTime? = null

    fun withId(id: Long?) = apply { this.id = id }

    fun withSegmentId(segmentId: Long) = apply { this.segmentId = segmentId }

    fun withFieldName(fieldName: String) = apply { this.fieldName = fieldName }

    fun withOperator(operator: String) = apply { this.operator = operator }

    fun withValueType(valueType: String) = apply { this.valueType = valueType }

    fun withConditionValue(conditionValue: String) = apply { this.conditionValue = conditionValue }

    fun withPosition(position: Int) = apply { this.position = position }

    fun withCreatedAt(createdAt: LocalDateTime?) = apply { this.createdAt = createdAt }

    fun build(): SegmentCondition =
        SegmentCondition(
            id = id,
            segmentId = segmentId,
            fieldName = fieldName,
            operator = operator,
            valueType = valueType,
            conditionValue = conditionValue,
            position = position,
            createdAt = createdAt,
        )

    companion object {
        fun aCondition() = SegmentConditionFixtures()

        fun giveMeOne(): SegmentConditionFixtures {
            val id = Random.nextLong(1, 101)
            val segmentId = Random.nextLong(1, 101)
            val position = Random.nextInt(1, 11)
            return aCondition()
                .withId(id)
                .withSegmentId(segmentId)
                .withPosition(position)
        }

        fun anEmailCondition(): SegmentConditionFixtures =
            giveMeOne()
                .withFieldName("user.email")
                .withOperator("EQ")
                .withValueType("STRING")
                .withConditionValue("\"test@example.com\"")

        fun aUserIdCondition(): SegmentConditionFixtures =
            giveMeOne()
                .withFieldName("user.id")
                .withOperator("GT")
                .withValueType("NUMBER")
                .withConditionValue("100")

        fun aCreatedAtCondition(): SegmentConditionFixtures =
            giveMeOne()
                .withFieldName("user.createdAt")
                .withOperator("GT")
                .withValueType("DATETIME")
                .withConditionValue("\"2024-01-01T00:00:00\"")

        fun anInCondition(): SegmentConditionFixtures =
            giveMeOne()
                .withFieldName("user.email")
                .withOperator("IN")
                .withValueType("STRING")
                .withConditionValue("[\"admin@example.com\", \"test@example.com\"]")

        fun aBetweenCondition(): SegmentConditionFixtures =
            giveMeOne()
                .withFieldName("user.id")
                .withOperator("BETWEEN")
                .withValueType("NUMBER")
                .withConditionValue("[1, 100]")
    }
}
