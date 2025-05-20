package com.manage.crm.user.domain.cache

import com.manage.crm.user.event.RefreshTotalUsersCommand
import com.manage.crm.user.support.UserEventPublisher
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service

@Service
class UserCacheManager(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val userEventPublisher: UserEventPublisher
) {
    companion object {
        private const val SPLIT = "::"
        private const val USER_CACHE_KEY = "user"
        const val USER_CACHE_KEY_PREFIX = USER_CACHE_KEY + SPLIT
        const val TOTAL_USER_COUNT_KEY = USER_CACHE_KEY_PREFIX + "total"
        const val TOTAL_USER_COUNT_UPDATED_AT_KEY = TOTAL_USER_COUNT_KEY + SPLIT + "updatedAt"
        private const val REFRESH_INTERVAL_MS = 1000L * 60 * 60 * 3 // 3 hours
    }

    suspend fun totalUserCount(): Long {
        val count = redisTemplate.opsForValue()
            .get(TOTAL_USER_COUNT_KEY)
            .awaitSingle() as Long? ?: 0L
        val updatedAt = redisTemplate.opsForValue()
            .get(TOTAL_USER_COUNT_UPDATED_AT_KEY)
            .awaitSingle() as Long? ?: 0L
        if (updatedAt < System.currentTimeMillis() - REFRESH_INTERVAL_MS) {
            userEventPublisher.publish(RefreshTotalUsersCommand(count))
        }
        return count
    }

    suspend fun incrTotalUserCount(): Long {
        return incr(TOTAL_USER_COUNT_KEY)
    }

    suspend fun incr(key: String): Long {
        val newCount = redisTemplate.opsForValue()
            .increment(key)
            .awaitSingle()
        redisTemplate.opsForValue()
            .set(TOTAL_USER_COUNT_UPDATED_AT_KEY, System.currentTimeMillis())
            .awaitSingle()
        return newCount
    }

    suspend fun saveTotalUserCount(count: Long): Long {
        val isSaved = redisTemplate.opsForValue()
            .set(TOTAL_USER_COUNT_KEY, count)
            .awaitSingle()
        redisTemplate.opsForValue()
            .set(TOTAL_USER_COUNT_UPDATED_AT_KEY, System.currentTimeMillis())
            .awaitSingle()
        if (isSaved) {
            return count
        } else {
            throw IllegalStateException("Failed to save total user count")
        }
    }
}
