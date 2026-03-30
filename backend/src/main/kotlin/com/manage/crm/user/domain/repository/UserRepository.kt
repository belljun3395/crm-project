package com.manage.crm.user.domain.repository

import com.manage.crm.user.domain.User
import kotlinx.coroutines.flow.toList
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserRepository : CoroutineCrudRepository<User, Long>, UserCustomRepository {
    suspend fun findAllByIdIn(ids: List<Long>): List<User> = findAllById(ids).toList()

    suspend fun findByExternalId(externalId: String): User?
}
