package com.manage.crm.user.domain.vo

import kotlin.random.Random

class JsonFixtures private constructor() {
    private lateinit var value: String

    fun withValue(value: String): JsonFixtures = apply {
        this.value = value
    }
    fun build(): Json = Json(value)

    companion object {
        fun aJson(): JsonFixtures = JsonFixtures()

        fun giveMeOne(): JsonFixtures {
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
