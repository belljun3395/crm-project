package com.manage.crm.email.event.send.notification

import com.manage.crm.email.event.send.notification.handler.NotificationEmailSendTimeOutEventHandler
import com.manage.crm.email.event.send.notification.handler.NotificationEmailSendTimeOutInvokeEventHandler
import com.manage.crm.support.coroutine.eventListenerCoroutineScope
import com.manage.crm.support.transactional.TransactionTemplates
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class NotificationEmailSendTimeOutEventListener(
    private val notificationEmailSendTimeOutEventHandler: NotificationEmailSendTimeOutEventHandler,
    private val notificationEmailSendTimeOutInvokeEventHandler: NotificationEmailSendTimeOutInvokeEventHandler,
    private val transactionalTemplates: TransactionTemplates
) {
    @EventListener
    fun onEvent(event: NotificationEmailSendTimeOutEvent) {
        eventListenerCoroutineScope().launch {
            transactionalTemplates.newTxWriter.executeAndAwait {
                notificationEmailSendTimeOutEventHandler.handle(event)
            }
        }
    }

    @EventListener
    fun onEvent(event: NotificationEmailSendTimeOutInvokeEvent) {
        eventListenerCoroutineScope().launch {
            transactionalTemplates.newTxWriter.executeAndAwait {
                notificationEmailSendTimeOutInvokeEventHandler.handle(event)
            }
        }
    }
}
