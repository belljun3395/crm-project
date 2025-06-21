package com.manage.crm.user.domain

import com.manage.crm.user.domain.vo.Json
import com.manage.crm.user.domain.vo.JsonFixtures
import java.time.LocalDateTime
import kotlin.random.Random

class UserFixtures private constructor() {
    private var id: Long = -1L
    private lateinit var externalId: String
    private lateinit var userAttributes: Json
    private lateinit var createdAt: LocalDateTime
    private lateinit var updatedAt: LocalDateTime

    fun withId(id: Long) = apply { this.id = id }
    fun withExternalId(externalId: String) = apply { this.externalId = externalId }
    fun withUserAttributes(userAttributes: Json) = apply { this.userAttributes = userAttributes }
    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }
    fun withUpdatedAt(updatedAt: LocalDateTime) = apply { this.updatedAt = updatedAt }

    fun build(): User = User(
        id = id,
        externalId = externalId,
        userAttributes = userAttributes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun anUser() = UserFixtures()
            .withCreatedAt(LocalDateTime.now())
            .withUpdatedAt(LocalDateTime.now())

        fun giveMeOne(): UserFixtures {
            val id = Random.nextLong(1, 101)
            val externalId = Random.nextLong(1, 101)
            val attributes = JsonFixtures.giveMeOne().withValue(
                """
                {
                    "email": "example$externalId@example.com"
                }
                """.trimIndent()
            ).build()
            return anUser()
                .withId(id)
                .withExternalId(externalId.toString())
                .withUserAttributes(attributes)
        }
    }
}
