package com.manage.crm.email.event.send.notification

import com.manage.crm.email.event.send.notification.handler.NotificationEmailSendTimeOutEventHandler
import com.manage.crm.email.event.send.notification.handler.NotificationEmailSendTimeOutInvokeEventHandler
import com.manage.crm.support.transactional.TransactionTemplates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class NotificationEmailSendTimeOutEventListener(
    private val notificationEmailSendTimeOutEventHandler: NotificationEmailSendTimeOutEventHandler,
    private val notificationEmailSendTimeOutInvokeEventHandler: NotificationEmailSendTimeOutInvokeEventHandler,
    private val transactionalTemplates: TransactionTemplates
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    @Async
    @EventListener
    fun onEvent(event: NotificationEmailSendTimeOutEvent) {
        scope.launch {
            transactionalTemplates.newTxWriter.executeAndAwait {
                notificationEmailSendTimeOutEventHandler.handle(event)
            }
        }
    }

    @Async
    @EventListener
    fun onEvent(event: NotificationEmailSendTimeOutInvokeEvent) {
        scope.launch {
            transactionalTemplates.newTxWriter.executeAndAwait {
                notificationEmailSendTimeOutInvokeEventHandler.handle(event)
            }
        }
    }
}
