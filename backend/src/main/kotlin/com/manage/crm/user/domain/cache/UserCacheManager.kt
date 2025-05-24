package com.manage.crm.user.domain.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class UserCacheManager(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>
) {
    companion object {
        val log = KotlinLogging.logger { }

        private const val REFRESH_INTERVAL_MS = 1000L * 60 * 60 * 3 // 3 hours
        private const val SPLIT = "::"
        private const val USER_CACHE_KEY = "user"
        const val USER_CACHE_KEY_PREFIX = USER_CACHE_KEY + SPLIT
        const val TOTAL_USER_COUNT_KEY = USER_CACHE_KEY_PREFIX + "total"
        const val TOTAL_USER_COUNT_UPDATED_AT_KEY = TOTAL_USER_COUNT_KEY + SPLIT + "updatedAt"

        fun isTotalUserCountNeedUpdate(totalUserCountUpdatedAt: Long): Boolean {
            val currentTime = System.currentTimeMillis()
            if (currentTime - totalUserCountUpdatedAt > REFRESH_INTERVAL_MS) {
                log.debug { "Total user count need update" }
                return true
            }
            log.debug { "Total user count not need update" }
            return false
        }
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

    suspend fun executeWithLock(key: String, ttlSeconds: Long = 10, block: suspend () -> Unit) {
        val lockKey = "$key::lock"
        val lockValue = System.currentTimeMillis().toString()
        val lock = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(ttlSeconds))
            .awaitSingleOrNull()
        if (lock == true) {
            try {
                block()
            } finally {
                val currentValue = redisTemplate.opsForValue().get(lockKey).awaitSingleOrNull()
                if (currentValue == lockValue) {
                    redisTemplate.delete(lockKey).awaitSingleOrNull()
                }
            }
        } else {
            log.warn { "Failed to acquire lock for key: $key" }
        }
    }

    suspend fun refreshTotalUserCountWithLock(block: suspend () -> Unit) {
        executeWithLock(TOTAL_USER_COUNT_KEY) {
            block()
        }
    }
}
