package com.manage.crm.event.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.assertions.throwables.shouldThrow

class OperationTest : BehaviorSpec({
    given("Operation#fromValue") {
        `when`("known symbol is provided") {
            then("returns matching operation") {
                Operation.fromValue("=") shouldBe Operation.EQUALS
                Operation.fromValue(">=") shouldBe Operation.GREATER_THAN_OR_EQUALS
                Operation.fromValue("<") shouldBe Operation.LESS_THAN
            }
        }

        `when`("known text operation is provided case-insensitively") {
            then("returns matching operation") {
                Operation.fromValue("like") shouldBe Operation.LIKE
                Operation.fromValue("between") shouldBe Operation.BETWEEN
            }
        }

        `when`("unknown operation is provided") {
            then("throws clear exception") {
                val ex = shouldThrow<IllegalArgumentException> {
                    Operation.fromValue("contains")
                }
                ex shouldHaveMessage "Invalid operation: contains"
            }
        }
    }
})
