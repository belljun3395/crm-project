package com.manage.crm.user.domain

import com.manage.crm.user.domain.vo.UserAttributes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("users")
class User(
    @Id
    var id: Long? = null,
    @Column("external_id")
    var externalId: String,
    @Column("user_attributes")
    var userAttributes: UserAttributes,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            externalId: String,
            userAttributes: UserAttributes
        ): User {
            return User(
                externalId = externalId,
                userAttributes = userAttributes
            )
        }

        fun new(
            id: Long,
            externalId: String,
            userAttributes: UserAttributes,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ): User {
            return User(
                id = id,
                externalId = externalId,
                userAttributes = userAttributes,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }

    /**
     * Update user attributes
     */
    fun updateAttributes(attributes: UserAttributes) {
        userAttributes = attributes
    }

    /**
     * Check if the user is new
     */
    fun isNewUser(): Boolean {
        return id == null
    }
}
