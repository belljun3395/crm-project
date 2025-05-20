package com.manage.crm.user.application.service

import com.manage.crm.user.domain.cache.UserCacheManager
import org.springframework.stereotype.Service

@Service
class CacheUserServiceImpl(
    private val userCacheManager: UserCacheManager
) : UserService {

    override suspend fun getTotalUserCount(): Long {
        return userCacheManager.totalUserCount()
    }

    override suspend fun incrementTotalUserCount(): Long {
        return userCacheManager.incrTotalUserCount()
    }
}
