package com.manage.crm.user.domain

import com.manage.crm.user.domain.vo.Json
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class UserTest : FeatureSpec({
    val id = 1L
    val externalId = "1"
    val attributes = Json(
        """
        {
            "email": "example@example.com"
        }
        """.trimIndent()
    )
    val user = User(
        id = id,
        externalId = externalId,
        userAttributes = attributes,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    feature("User#updateAttributes") {
        scenario("update user attributes") {
            // given
            val newAttributes = Json(
                """
                {
                    "email": "new@example.com"
               }
                """.trimIndent()
            )

            // when
            user.updateAttributes(newAttributes)

            // then
            user.userAttributes shouldBe newAttributes
        }
    }
})
