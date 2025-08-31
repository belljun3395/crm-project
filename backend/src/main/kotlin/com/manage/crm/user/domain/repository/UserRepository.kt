package com.manage.crm.user.domain.repository

import com.manage.crm.user.domain.User
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.query.Param

interface UserRepository : CoroutineCrudRepository<User, Long>, UserRepositoryCustom {
    @Query(
        """
            SELECT * FROM users WHERE users.USER_ATTRIBUTES LIKE '%'||:key||'%'
        """
    )
    suspend fun findAllExistByUserAttributesKey(@Param("key") key: String? = "email"): List<User>

    suspend fun findAllByIdIn(ids: List<Long>): List<User>

    suspend fun findByExternalId(externalId: String): User?
}
