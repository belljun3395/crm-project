package com.manage.crm.user.adapter.query

import com.manage.crm.user.application.port.query.UserReadModel
import com.manage.crm.user.application.port.query.UserReadPort
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Component

@Component
class UserReadAdapter(
    private val userRepository: UserRepository,
) : UserReadPort {
    override suspend fun findByExternalId(externalId: String): UserReadModel? = userRepository.findByExternalId(externalId)?.toQueryResult()

    override suspend fun findAllByIdIn(ids: Collection<Long>): List<UserReadModel> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        return userRepository.findAllByIdIn(ids.toList()).map { it.toQueryResult() }
    }

    override suspend fun findAll(): List<UserReadModel> = userRepository.findAll().toList().map { it.toQueryResult() }
}

private fun User.toQueryResult(): UserReadModel {
    val userId = requireNotNull(id) { "User id cannot be null for query result" }
    return UserReadModel(
        id = userId,
        externalId = externalId,
        userAttributesJson = userAttributes.value,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
