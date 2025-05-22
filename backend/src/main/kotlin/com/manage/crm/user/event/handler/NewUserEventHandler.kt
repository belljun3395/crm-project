package com.manage.crm.user.event.handler

import com.manage.crm.user.application.service.UserService
import com.manage.crm.user.event.NewUserEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class NewUserEventHandler(
    private val userService: UserService
) {
    private val log = KotlinLogging.logger {}

    /**
     * - Increment Total User Count
     */
    suspend fun handle(event: NewUserEvent) {
        userService.incrementTotalUserCount().let {
            log.debug { "total user count: $it" }
        }
    }
}
