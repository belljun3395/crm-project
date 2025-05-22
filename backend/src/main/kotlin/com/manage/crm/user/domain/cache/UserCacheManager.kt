package com.manage.crm.user.domain.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service

@Service
class UserCacheManager(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>
) {
    val log = KotlinLogging.logger { }

    companion object {
        private const val SPLIT = "::"
        private const val USER_CACHE_KEY = "user"
        const val USER_CACHE_KEY_PREFIX = USER_CACHE_KEY + SPLIT
        const val TOTAL_USER_COUNT_KEY = USER_CACHE_KEY_PREFIX + "total"
        const val TOTAL_USER_COUNT_UPDATED_AT_KEY = TOTAL_USER_COUNT_KEY + SPLIT + "updatedAt"
    }

    suspend fun totalUserCount(): Long {
        return redisTemplate.opsForValue()
            .get(TOTAL_USER_COUNT_KEY)
            .awaitSingleOrNull() as Long? ?: run {
            log.warn { "Failed to get total user count" }
            return 0L
        }
    }

    suspend fun totalUserCountUpdatedAt(): Long {
        return redisTemplate.opsForValue()
            .get(TOTAL_USER_COUNT_UPDATED_AT_KEY)
            .awaitSingleOrNull() as Long? ?: run {
            log.warn { "Failed to get total user count" }
            return 0L
        }
    }

    suspend fun incrTotalUserCount(): Long {
        return incr(TOTAL_USER_COUNT_KEY)
    }

    suspend fun incr(key: String): Long {
        val newCount = redisTemplate.opsForValue()
            .increment(key)
            .awaitSingleOrNull() ?: run {
            log.warn { "Failed to get total user count" }
            return 0L
        }

        redisTemplate.opsForValue()
            .set(TOTAL_USER_COUNT_UPDATED_AT_KEY, System.currentTimeMillis())
            .awaitSingleOrNull() ?: log.warn { "Failed to update total user count updated at" }
        return newCount
    }

    suspend fun saveTotalUserCount(count: Long): Long {
        redisTemplate.opsForValue()
            .set(TOTAL_USER_COUNT_KEY, count)
            .awaitSingleOrNull() ?: run {
            log.warn { "Failed to save total user count" }
        }
        return count
    }

    suspend fun updateTotalUserCountUpdateAt(): Long {
        val currentTime = System.currentTimeMillis()
        redisTemplate.opsForValue()
            .set(TOTAL_USER_COUNT_UPDATED_AT_KEY, currentTime)
            .awaitSingleOrNull() ?: run {
            log.warn { "Failed to update total user count updated at" }
        }
        return currentTime
    }
}
