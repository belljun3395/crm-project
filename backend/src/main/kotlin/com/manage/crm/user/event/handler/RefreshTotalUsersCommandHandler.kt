package com.manage.crm.user.event.handler

import com.manage.crm.user.domain.cache.UserCacheManager
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.event.RefreshTotalUsersCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class RefreshTotalUsersCommandHandler(
    private val userRepository: UserRepository,
    private val userCacheManager: UserCacheManager
) {
    val log = KotlinLogging.logger { }

    suspend fun handle(command: RefreshTotalUsersCommand) {
        val oldTotalUsers = command.oldTotalUsers
        userCacheManager.refreshTotalUserCountWithLock {
            // check if the total user count is updated already
            val totalUserCountUpdatedAt = userCacheManager.totalUserCountUpdatedAt()
            if (UserCacheManager.isTotalUserCountNeedUpdate(totalUserCountUpdatedAt)) {
                userRepository.count().let { count ->
                    userCacheManager.saveTotalUserCount(count).let {
                        log.debug { "refresh total users: $oldTotalUsers -> $it" }
                    }
                }
            } else {
                log.debug { "total user count not need update" }
            }
        }
    }
}
