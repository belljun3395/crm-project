package com.manage.crm.user.application.service

import com.manage.crm.user.domain.cache.UserCacheManager
import com.manage.crm.user.event.RefreshTotalUsersCommand
import com.manage.crm.user.support.UserEventPublisher
import org.springframework.stereotype.Service

@Service
class CacheUserServiceImpl(
    private val userCacheManager: UserCacheManager,
    private val userEventPublisher: UserEventPublisher
) : UserService {
    companion object {
        private const val REFRESH_INTERVAL_MS = 1000L * 60 * 60 * 3 // 3 hours
    }

    override suspend fun getTotalUserCount(): Long {
        val totalUserCount = userCacheManager.totalUserCount()
        val totalUserCountUpdatedAt = userCacheManager.totalUserCountUpdatedAt()
        val currentTime = System.currentTimeMillis()
        if (currentTime - totalUserCountUpdatedAt > REFRESH_INTERVAL_MS) {
            userEventPublisher.publishEvent(RefreshTotalUsersCommand(totalUserCount))
        }
        return totalUserCount
    }

    override suspend fun incrementTotalUserCount(): Long {
        return userCacheManager.incrTotalUserCount().apply {
            userCacheManager.updateTotalUserCountUpdateAt()
        }
    }
}
