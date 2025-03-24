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
            val variables = Variables("attribute_title:hello", "attribute_name")

            // when
            val result = variables.getVariables()

            // then
            result shouldBe listOf("attribute_title:hello", "attribute_name")
        }

        scenario("get variables without default") {
            // given
            val variables = Variables("attribute_title:hello", "attribute_name")

            // when
            val result = variables.getVariables(withDefault = false)

            // then
            result shouldBe listOf("attribute_title", "attribute_name")
        }
    }

    feature("Variables#findVariable") {
        scenario("get variable") {
            // given
            val variables = Variables("attribute_title:hello", "attribute_name")

            // when
            val result = variables.findVariable("attribute_title")

            // then
            result shouldBe "attribute_title:hello"
        }

        scenario("get variable without default") {
            // given
            val variables = Variables("attribute_title:hello", "attribute_name")

            // when
            val result = variables.findVariable("attribute_title", withDefault = false)

            // then
            result shouldBe "attribute_title"
        }
    }

    feature("Variables#findVariableDefault") {
        scenario("get variable default") {
            // given
            val variables = Variables("attribute_title:hello", "attribute_name")

            // when
            val result = variables.findVariableDefault("attribute_title")

            // then
            result shouldBe "hello"
        }

        scenario("get variable default which does not have default value") {
            // given
            val variables = Variables("attribute_title:hello", "attribute_name")

            // when
            val result = variables.findVariableDefault("attribute_name")

            // then
            result shouldBe null
        }
    }
})
