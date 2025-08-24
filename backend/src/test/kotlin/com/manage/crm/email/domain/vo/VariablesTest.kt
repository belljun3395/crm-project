package com.manage.crm.email.domain.vo

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class VariablesTest : FeatureSpec({

    feature("Variables#_checkValueContainType") {
        scenario("throw exception when value does not contain attribute type") {
            // when
            val exception = runCatching {
                Variables("title:hello", "name")
            }.exceptionOrNull()

            // then
            exception shouldBe IllegalArgumentException("Value need to contain _ for distinguish variable type.")
        }

        scenario("throw exception when custom attribute format is invalid") {
            // when
            val exception = runCatching {
                Variables("custom_title_hello", "custom_name_test")
            }.exceptionOrNull()

            // then
            exception shouldBe IllegalArgumentException("Custom type format is invalid.")
        }
    }

    feature(("Variables#isEmpty")) {
        scenario("check if variables is empty") {
            // given
            val variables = Variables()

            // then
            variables.isEmpty() shouldBe true
        }
    }

    feature("Variables#getVariables") {
        scenario("get attribute variables") {
            // given
            val variables = Variables("attribute_title:hello", "attribute_name")

            // when
            val result = variables.getVariables()

            // then
            result shouldBe listOf("attribute_title:hello", "attribute_name")
        }

        scenario("get custom variables") {
            // given
            val variables = Variables("custom_title:hello", "custom_name")

            // when
            val result = variables.getVariables()

            // then
            result shouldBe listOf("custom_title:hello", "custom_name")
        }

        scenario("get variables without default") {
            // given
            val variables = Variables("attribute_title:hello", "attribute_name")

            // when
            val result = variables.getVariables(withDefault = false)

            // then
            result shouldBe listOf("attribute_title", "attribute_name")
        }

        scenario("get custom variables without default") {
            // given
            val variables = Variables("custom_title:hello", "custom_name")

            // when
            val result = variables.getVariables(withDefault = false)

            // then
            result shouldBe listOf("custom_title", "custom_name")
        }
    }

    feature("Variables#findVariable") {
        scenario("get attribute  variable") {
            // given
            val variables = Variables("attribute_title:hello", "attribute_name")

            // when
            val result = variables.findVariable("attribute_title")

            // then
            result shouldBe "attribute_title:hello"
        }

        scenario("get custom  variable") {
            // given
            val variables = Variables("custom_title:hello", "custom_name")

            // when
            val result = variables.findVariable("custom_title")

            // then
            result shouldBe "custom_title:hello"
        }

        scenario("get attribute variable without default") {
            // given
            val variables = Variables("attribute_title:hello", "attribute_name")

            // when
            val result = variables.findVariable("attribute_title", withDefault = false)

            // then
            result shouldBe "attribute_title"
        }

        scenario("get custom variable without default") {
            // given
            val variables = Variables("custom_title:hello", "custom_name")

            // when
            val result = variables.findVariable("custom_title", withDefault = false)

            // then
            result shouldBe "custom_title"
        }
    }

    feature("Variables#findVariableDefault") {
        scenario("get attribute variable default") {
            // given
            val variables = Variables("attribute_title:hello", "attribute_name")

            // when
            val result = variables.findVariableDefault("attribute_title")

            // then
            result shouldBe "hello"
        }

        scenario("get custom variable default") {
            // given
            val variables = Variables("custom_title:hello", "custom_name")

            // when
            val result = variables.findVariableDefault("custom_title")

            // then
            result shouldBe "hello"
        }

        scenario("get attribute variable default which does not have default value") {
            // given
            val variables = Variables("attribute_title:hello", "attribute_name")

            // when
            val result = variables.findVariableDefault("attribute_name")

            // then
            result shouldBe null
        }

        scenario("get custom variable default which does not have default value") {
            // given
            val variables = Variables("custom_title:hello", "custom_name")

            // when
            val result = variables.findVariableDefault("custom_name")

            // then
            result shouldBe null
        }
    }
})
