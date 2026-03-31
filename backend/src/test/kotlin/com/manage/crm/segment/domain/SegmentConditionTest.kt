package com.manage.crm.segment.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class SegmentConditionTest : BehaviorSpec({
    given("SegmentCondition#new") {
        `when`("creating condition row") {
            val condition = SegmentCondition.new(
                segmentId = 30L,
                fieldName = "user.id",
                operator = "GT",
                valueType = "NUMBER",
                conditionValue = "100",
                position = 1
            )

            then("initializes condition payload") {
                condition.segmentId shouldBe 30L
                condition.fieldName shouldBe "user.id"
                condition.operator shouldBe "GT"
                condition.valueType shouldBe "NUMBER"
                condition.conditionValue shouldBe "100"
                condition.position shouldBe 1
            }

            then("leaves persistence-managed fields unset") {
                condition.id.shouldBeNull()
                condition.createdAt.shouldBeNull()
            }
        }
    }
})
