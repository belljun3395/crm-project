package com.manage.crm.email.event.template

import com.manage.crm.email.event.template.handler.PostEmailTemplateEventHandler
import com.manage.crm.support.transactional.TransactionTemplates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.reactive.executeAndAwait

@Component
class EventTemplateTransactionListener(
    private val postEmailTemplateEventHandler: PostEmailTemplateEventHandler,
    private val transactionalTemplates: TransactionTemplates
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    fun handleAfterCompletionEvent(event: EmailTemplateTransactionAfterCompletionEvent) {
        scope.launch {
            transactionalTemplates.newTxWriter.executeAndAwait {
                when (event) {
                    is PostEmailTemplateEvent -> postEmailTemplateEventHandler.handle(event)
                }
            }
        }
    }
}
