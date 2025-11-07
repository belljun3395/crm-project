package com.manage.crm.user.domain.repository

import com.manage.crm.user.domain.User
import org.springframework.data.repository.query.Param

interface UserRepositoryCustom {
    suspend fun findByEmail(@Param("email") email: String): User?

    suspend fun findAllWithPagination(page: Int, size: Int): List<User>

    suspend fun countAll(): Long
}
