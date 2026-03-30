package com.manage.crm.event.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage

class JoinOperationTest : BehaviorSpec({
    given("JoinOperation#fromValue") {
        `when`("known join operation is provided") {
            then("returns matching enum") {
                JoinOperation.fromValue("AND") shouldBe JoinOperation.AND
                JoinOperation.fromValue("or") shouldBe JoinOperation.OR
                JoinOperation.fromValue("end") shouldBe JoinOperation.END
            }
        }

        `when`("unknown join operation is provided") {
            then("throws clear exception") {
                val ex = shouldThrow<IllegalArgumentException> {
                    JoinOperation.fromValue("XOR")
                }
                ex shouldHaveMessage "Invalid operation: XOR"
            }
        }
    }
})
