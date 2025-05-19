package com.manage.crm.user.event.handler

import com.manage.crm.user.domain.cache.UserCacheManager
import com.manage.crm.user.event.NewUserEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class NewUserEventHandler(
    private val userCacheManager: UserCacheManager
) {
    private val log = KotlinLogging.logger {}

    /**
     * - Increment Total User Count
     */
    suspend fun handle(event: NewUserEvent) {
        userCacheManager.incrTotalUserCount().let {
            log.debug { "total user count: $it" }
        }
    }
}
