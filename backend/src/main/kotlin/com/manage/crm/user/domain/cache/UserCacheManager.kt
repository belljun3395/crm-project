package com.manage.crm.user.domain.cache

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service

@Service
class UserCacheManager(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>
) {
    companion object {
        const val TOTAL_USER_COUNT_KEY = "user::total"
    }

    suspend fun incrTotalUserCount(): Long {
        return incr(TOTAL_USER_COUNT_KEY)
    }

    suspend fun incr(key: String): Long {
        return redisTemplate.opsForValue()
            .increment(key)
            .awaitSingle()
    }
}
