package com.manage.crm.user.domain.vo

import kotlin.random.Random

class UserAttributesFixtures private constructor() {
    private var value: String = "{}"

    fun withValue(value: String): UserAttributesFixtures = apply {
        this.value = value
    }
    fun build(): UserAttributes = UserAttributes(value)

    companion object {
        fun aJson(): UserAttributesFixtures = UserAttributesFixtures()

        fun giveMeOne(): UserAttributesFixtures {
            val externalId = Random.nextLong(1, 101)
            val value =
                """
                {
                    "email": "example$externalId@example.com"
                }
                """.trimIndent()
            return aJson()
                .withValue(value)
        }
    }
}
