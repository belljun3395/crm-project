package com.manage.crm.user.application.service

import com.manage.crm.user.domain.cache.UserCacheManager
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.event.RefreshTotalUsersCommand
import com.manage.crm.user.support.UserEventPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class CacheUserServiceImpl(
    private val userCacheManager: UserCacheManager,
    private val userEventPublisher: UserEventPublisher,
    private val userRepository: UserRepository
) : UserService {
    private val log = KotlinLogging.logger {}

    override suspend fun getTotalUserCount(): Long {
        return runCatching {
            val totalUserCount = userCacheManager.totalUserCount()
            val totalUserCountUpdatedAt = userCacheManager.totalUserCountUpdatedAt()
            if (UserCacheManager.isTotalUserCountNeedUpdate(totalUserCountUpdatedAt)) {
                userEventPublisher.publishEvent(RefreshTotalUsersCommand(totalUserCount))
            }
            totalUserCount
        }.getOrElse { error ->
            log.warn(error) { "Falling back to repository count because cache lookup failed" }
            userRepository.count().also { freshCount ->
                runCatching {
                    userCacheManager.saveTotalUserCount(freshCount)
                    userCacheManager.updateTotalUserCountUpdateAt()
                }.onFailure { cacheUpdateError ->
                    log.warn(cacheUpdateError) { "Failed to refresh total user count cache after repository fallback" }
                }
            }
        }
    }

    override suspend fun incrementTotalUserCount(): Long {
        return userCacheManager.incrTotalUserCount().apply {
            userCacheManager.updateTotalUserCountUpdateAt()
        }
    }
}
