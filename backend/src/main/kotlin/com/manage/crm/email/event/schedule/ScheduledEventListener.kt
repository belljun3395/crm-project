package com.manage.crm.email.event.schedule

import com.manage.crm.email.event.schedule.handler.CancelScheduledEventHandler
import com.manage.crm.email.support.EmailCoroutineScope.eventListenerScope
import com.manage.crm.support.transactional.TransactionTemplates
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class ScheduledEventListener(
    private val cancelScheduledEventHandler: CancelScheduledEventHandler,
    private val transactionalTemplates: TransactionTemplates
) {
    @EventListener
    fun onCancelEvent(event: CancelScheduledEvent) {
        eventListenerScope().launch {
            transactionalTemplates.newTxWriter.executeAndAwait {
                cancelScheduledEventHandler.handle(event)
            }
        }
    }
}
