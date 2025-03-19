package com.manage.crm.email.domain.vo

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class VariablesTest : FeatureSpec({

    feature(("Variables#isEmpty")) {
        scenario("check if variables is empty") {
            // given
            val variables = Variables()

            // then
            variables.isEmpty() shouldBe true
        }
    }

    feature("Variables#getVariables") {
        scenario("get variables") {
            // given
            val variables = Variables("title:hello", "name")

            // when
            val result = variables.getVariables()

            // then
            result shouldBe listOf("title:hello", "name")
        }

        scenario("get variables without default") {
            // given
            val variables = Variables("title:hello", "name")

            // when
            val result = variables.getVariables(withDefault = false)

            // then
            result shouldBe listOf("title", "name")
        }
    }

    feature("Variables#findVariable") {
        scenario("get variable") {
            // given
            val variables = Variables("title:hello", "name")

            // when
            val result = variables.findVariable("title")

            // then
            result shouldBe "title:hello"
        }

        scenario("get variable without default") {
            // given
            val variables = Variables("title:hello", "name")

            // when
            val result = variables.findVariable("title", withDefault = false)

            // then
            result shouldBe "title"
        }
    }
})
