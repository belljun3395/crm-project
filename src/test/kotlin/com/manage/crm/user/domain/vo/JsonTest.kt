package com.manage.crm.user.domain.vo

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class JsonTest : FeatureSpec({
    val json = Json(
        """
                {
                    "email": "example@example.com"
                }
        """.trimIndent()
    )
    feature("Json#getValue") {
        scenario("valid key and objectMapper are provided") {
            // given
            val key = RequiredUserAttributeKey.EMAIL
            val objectMapper = ObjectMapper()

            // when
            val result = json.getValue(key, objectMapper)

            // then
            result shouldBe "example@example.com"
        }

        scenario("invalid key are provided") {
            // given
            val key = RequiredUserAttributeKey.NAME
            val objectMapper = ObjectMapper()

            // when
            val exception = shouldThrow<NullPointerException> {
                json.getValue(key, objectMapper)
            }

            // then
            exception.message shouldBe "null cannot be cast to non-null type kotlin.String"
        }
    }
})
