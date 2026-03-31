package com.manage.crm.user.application.port.query

import java.time.LocalDateTime

data class UserReadModel(
    val id: Long,
    val externalId: String,
    val userAttributesJson: String,
    val createdAt: LocalDateTime?
)

interface UserReadPort {
    suspend fun findByExternalId(externalId: String): UserReadModel?

    suspend fun findAllByIdIn(ids: Collection<Long>): List<UserReadModel>

    suspend fun findAll(): List<UserReadModel>
}
