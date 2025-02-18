package com.manage.crm.email.event.schedule

import com.manage.crm.email.event.schedule.handler.CancelScheduledEventHandler
import com.manage.crm.support.transactional.TransactionTemplates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class ScheduledEventListener(
    private val cancelScheduledEventHandler: CancelScheduledEventHandler,
    private val transactionalTemplates: TransactionTemplates
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    @Async
    @EventListener
    fun onCancelEvent(event: CancelScheduledEvent) {
        scope.launch {
            transactionalTemplates.newTxWriter.executeAndAwait {
                cancelScheduledEventHandler.handle(event)
            }
        }
    }
}
