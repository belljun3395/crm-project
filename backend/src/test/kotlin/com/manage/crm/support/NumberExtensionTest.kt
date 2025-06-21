package com.manage.crm.support

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NumberExtensionTest : FunSpec({

    test("Int should be converted to Long") {
        val intValue = 123
        intValue.asLong() shouldBe 123L
    }

    test("Long should remain Long") {
        val longValue = 456L
        longValue.asLong() shouldBe 456L
    }

    test("Valid String should be converted to Long") {
        val stringValue = "789"
        stringValue.asLong() shouldBe 789L
    }

    test("Invalid String should throw NumberFormatException") {
        val invalidString = "abc"
        shouldThrow<NumberFormatException> {
            invalidString.asLong()
        }
    }

    test("Unsupported type should throw IllegalArgumentException") {
        val doubleValue = 123.45
        shouldThrow<IllegalArgumentException> {
            doubleValue.asLong()
        }
    }

    test("null should throw IllegalArgumentException") {
        val nullValue: Any? = null
        shouldThrow<IllegalArgumentException> {
            nullValue.asLong()
        }
    }

    test("Custom object should throw IllegalArgumentException") {
        val customObject = object {}
        shouldThrow<IllegalArgumentException> {
            customObject.asLong()
        }
    }
})
