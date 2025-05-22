package com.manage.crm.user.event

import com.manage.crm.support.coroutine.eventListenerCoroutineScope
import com.manage.crm.user.event.handler.RefreshTotalUsersCommandHandler
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class UserEventListener(
    val refreshTotalUsersCommandHandler: RefreshTotalUsersCommandHandler
) {

    @EventListener
    fun handleEvent(event: UserEvent) {
        eventListenerCoroutineScope().apply {
            when (event) {
                is RefreshTotalUsersCommand -> launch {
                    refreshTotalUsersCommandHandler.handle(event)
                }
            }
        }
    }
}
