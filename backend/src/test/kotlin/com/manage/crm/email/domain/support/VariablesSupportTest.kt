package com.manage.crm.email.domain.support

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.user.domain.vo.Json
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class VariablesSupportTest : FeatureSpec({

    val objectMapper = ObjectMapper()

    feature("VariablesSupport#doAssociate") {
        scenario("associate with attribute key that exists in attributes") {
            // given
            val key = "attribute_name"
            val attributes = Json("""{"attribute_name": "John Doe"}""")
            val variables = Variables("attribute_name:defaultName")

            // when
            val result = VariablesSupport.doAssociate(objectMapper, key, attributes, variables)

            // then
            result shouldBe ("attribute_name" to "John Doe")
        }

        scenario("associate with attribute key that does not exist in attributes") {
            // given
            val key = "attribute_name"
            val attributes = Json("""{"attribute_email": "john@example.com"}""")
            val variables = Variables("attribute_name:defaultName")

            // when
            val result = VariablesSupport.doAssociate(objectMapper, key, attributes, variables)

            // then
            result shouldBe ("attribute_name" to "defaultName")
        }

        scenario("associate with custom attribute key that exists in attributes") {
            // given
            val key = "custom_title"
            val attributes = Json("""{"custom_title": "Manager"}""")
            val variables = Variables("custom_title:defaultTitle")

            // when
            val result = VariablesSupport.doAssociate(objectMapper, key, attributes, variables)

            // then
            result shouldBe ("custom_title" to "Manager")
        }

        scenario("associate with custom attribute key that does not exist in attributes") {
            // given
            val key = "custom_title"
            val attributes = Json("""{"custom_name": "John Doe"}""")
            val variables = Variables("custom_title:defaultTitle")

            // when
            val result = VariablesSupport.doAssociate(objectMapper, key, attributes, variables)

            // then
            result shouldBe ("custom_title" to "defaultTitle")
        }

        scenario("associate with key that has no default value in variables") {
            // given
            val key = "attribute_name"
            val attributes = Json("""{"attribute_email": "john@example.com"}""")
            val variables = Variables("attribute_email:defaultEmail")

            // when
            val result = VariablesSupport.doAssociate(objectMapper, key, attributes, variables)

            // then
            result shouldBe ("attribute_name" to "")
        }
    }

    feature("VariablesSupport#variablesAllMatchedWithKey") {
        scenario("all variables are matched with keys") {
            // given
            val variables = Variables("attribute_name:John", "custom_title:Manager")
            val keys = setOf("attribute_name", "custom_title")

            // when
            val result = VariablesSupport.variablesAllMatchedWithKey(variables, keys)

            // then
            result shouldBe true
        }

        scenario("some variables are not matched with keys") {
            // given
            val variables = Variables("attribute_name:John", "custom_title:Manager", "attribute_email:test@test.com")
            val keys = setOf("attribute_name", "custom_title")

            // when
            val result = VariablesSupport.variablesAllMatchedWithKey(variables, keys)

            // then
            result shouldBe false
        }

        scenario("empty variables should return false") {
            // given
            val variables = Variables()
            val keys = setOf("attribute_name", "custom_title")

            // when
            val result = VariablesSupport.variablesAllMatchedWithKey(variables, keys)

            // then
            result shouldBe false
        }

        scenario("empty keys should return false when variables exist") {
            // given
            val variables = Variables("attribute_name:John")
            val keys = emptySet<String>()

            // when
            val result = VariablesSupport.variablesAllMatchedWithKey(variables, keys)

            // then
            result shouldBe false
        }
    }
})
