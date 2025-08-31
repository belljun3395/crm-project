package com.manage.crm.user.domain.vo

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class JsonTest : FeatureSpec({
    val userAttributes = UserAttributes(
        """
                {
                    "email": "example@example.com",
                    "gender": "male",
                    "detail": "{\"age\" : 20}"
                }
        """.trimIndent()
    )
    feature("Json#isExist") {
        scenario("valid key and objectMapper are provided") {
            // given
            val key = "email"
            val objectMapper = ObjectMapper()

            // when
            val result = userAttributes.isExist(key, objectMapper)

            // then
            result shouldBe true
        }

        scenario("not exist key and objectMapper are provided") {
            // given
            val key = "name"
            val objectMapper = ObjectMapper()

            // when
            val result = userAttributes.isExist(key, objectMapper)

            // then
            result shouldBe false
        }

        scenario("valid keys and objectMapper are provided") {
            // given
            val keys = listOf("detail", "age")
            val objectMapper = ObjectMapper()

            // when
            val result = userAttributes.isExist(keys, objectMapper)

            // then
            result shouldBe true
        }

        scenario("not exist keys and objectMapper are provided") {
            // given
            val keys = listOf("detail", "name")
            val objectMapper = ObjectMapper()

            // when
            val result = userAttributes.isExist(keys, objectMapper)

            // then
            result shouldBe false
        }
    }

    feature("Json#getValue") {
        scenario("required attribute key and objectMapper are provided") {
            // given
            val key = RequiredUserAttributeKey.EMAIL
            val objectMapper = ObjectMapper()

            // when
            val result = userAttributes.getValue(key, objectMapper)

            // then
            result shouldBe "example@example.com"
        }

        scenario("valid key and objectMapper are provided") {
            // given
            val key = "gender"
            val objectMapper = ObjectMapper()

            // when
            val result = userAttributes.getValue(key, objectMapper)

            // then
            result shouldBe "male"
        }

        scenario("invalid key are provided") {
            // given
            val key = RequiredUserAttributeKey.NAME
            val objectMapper = ObjectMapper()

            // when
            val exception = shouldThrow<NullPointerException> {
                userAttributes.getValue(key, objectMapper)
            }

            // then
            exception.message shouldBe "null cannot be cast to non-null type kotlin.String"
        }

        scenario("valid keys and objectMapper are provided") {
            // given
            val keys = listOf("detail", "age")
            val objectMapper = ObjectMapper()

            // when
            val result = userAttributes.getValue(keys, objectMapper)

            // then
            result shouldBe "20"
        }

        scenario("invalid order keys and objectMapper are provided") {
            // given
            val keys = listOf("age", "detail")
            val objectMapper = ObjectMapper()

            // when
            val exception = shouldThrow<IllegalArgumentException> {
                userAttributes.getValue(keys, objectMapper)
            }

            // then
            exception.message shouldBe "Key not found. key: ${keys[0]}"
        }
    }
})
