package com.manage.crm.user.domain

import com.manage.crm.user.domain.vo.UserAttributesFixtures
import com.manage.crm.user.domain.vo.UserAttributes
import java.time.LocalDateTime
import kotlin.random.Random

class UserFixtures private constructor() {
    private var id: Long = -1L
    private var externalId: String = "default-external-id"
    private var userAttributes: UserAttributes = UserAttributesFixtures.giveMeOne().build()
    private var createdAt: LocalDateTime = LocalDateTime.now()
    private var updatedAt: LocalDateTime = LocalDateTime.now()

    fun withId(id: Long) = apply { this.id = id }
    fun withExternalId(externalId: String) = apply { this.externalId = externalId }
    fun withUserAttributes(userAttributes: UserAttributes) = apply { this.userAttributes = userAttributes }
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

        fun giveMeOne(): UserFixtures {
            val id = Random.nextLong(1, 101)
            val externalId = Random.nextLong(1, 101)
            val attributes = UserAttributesFixtures.giveMeOne().withValue(
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
