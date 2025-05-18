package com.manage.crm.email.event.template

import com.manage.crm.email.event.template.handler.PostEmailTemplateEventHandler
import com.manage.crm.support.coroutine.eventListenerCoroutineScope
import com.manage.crm.support.transactional.TransactionTemplates
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

// TODO fix naming
@Component
class EventTemplateTransactionListener(
    private val postEmailTemplateEventHandler: PostEmailTemplateEventHandler,
    private val transactionalTemplates: TransactionTemplates
) {
    /**
     * `@EventListener`를 사용한 이유:
     * `EmailEventPublisher`에서 트랜잭션에 `TransactionSynchronization`를 등록하는 방식으로 이벤트 발행 시점을 제어하고 있다.
     * @see com.manage.crm.email.support.EmailEventPublisher.publishEvent
     */
    @EventListener
    fun handleAfterCompletionEvent(event: EmailTemplateTransactionAfterCompletionEvent) {
        eventListenerCoroutineScope().launch {
            transactionalTemplates.newTxWriter.executeAndAwait {
                when (event) {
                    is PostEmailTemplateEvent -> postEmailTemplateEventHandler.handle(event)
                }
            }
        }
    }
}
