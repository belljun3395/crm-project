package com.manage.crm.support.web.idempotency

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class IdempotencyKeyPolicyTest : BehaviorSpec({
    given("IdempotencyKeyPolicy") {
        `when`("key has valid length and allowed charset") {
            then("it is valid") {
                IdempotencyKeyPolicy.isValid("abcDEF12-key:_segment") shouldBe true
            }
        }

        `when`("key is shorter than minimum length") {
            then("it is invalid") {
                IdempotencyKeyPolicy.isValid("short7") shouldBe false
            }
        }

        `when`("key includes unsupported characters") {
            then("it is invalid") {
                IdempotencyKeyPolicy.isValid("invalid key with spaces") shouldBe false
            }
        }
    }
})
