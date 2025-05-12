package com.manage.crm.user.domain.service.hash

import com.manage.crm.user.domain.User
import org.springframework.data.redis.hash.Jackson2HashMapper
import org.springframework.stereotype.Service

@Service
class User2HashMapper(
    private val mapper: Jackson2HashMapper
) {
    fun toHash(user: User): Map<String, Any> {
        return mapper.toHash(user)
    }

    fun loadHash(hash: Map<String, Any>): User {
        return mapper.fromHash(hash) as User
    }
}
