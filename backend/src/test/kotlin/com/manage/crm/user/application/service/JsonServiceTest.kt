package com.manage.crm.user.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.user.domain.vo.Json
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import com.manage.crm.user.exception.JsonException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class JsonServiceTest : FeatureSpec({

    val objectMapper = ObjectMapper()
    val jsonService = JsonService(objectMapper)
    feature("JsonService#execute") {
        scenario("attribute is JSON format and contains required key") {
            // given
            val attribute = """
            {
                "email": "example@example.com"
            }
            """.trimIndent()
            val keys = arrayOf(RequiredUserAttributeKey.EMAIL)

            // when
            val result = jsonService.execute(attribute, *keys)

            // then
            result shouldBe Json(attribute)
        }

        scenario("attribute is not JSON format") {
            // given
            val attribute = "email: example@example.com"
            val keys = arrayOf(RequiredUserAttributeKey.EMAIL)

            // when
            val exception = shouldThrow<IllegalArgumentException> {
                jsonService.execute(attribute, *keys)
            }

            // then
            exception.message shouldBe "Attribute is not JSON format: $attribute"
        }

        scenario("attribute is not contain required key") {
            // given
            val attribute = """
            {
                "name": "example"
            }
            """.trimIndent()
            val keys = arrayOf(RequiredUserAttributeKey.EMAIL)

            // when
            val exception = shouldThrow<JsonException> {
                jsonService.execute(attribute, *keys)
            }

            // then
            exception.message shouldBe "Attribute does not contain key: ${RequiredUserAttributeKey.EMAIL.value}"
        }
    }
})
