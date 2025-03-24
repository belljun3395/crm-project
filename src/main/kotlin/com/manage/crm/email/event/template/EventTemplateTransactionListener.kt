package com.manage.crm.email.event.template

import com.manage.crm.email.event.template.handler.PostEmailTemplateEventHandler
import com.manage.crm.support.transactional.TransactionTemplates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class EventTemplateTransactionListener(
    private val postEmailTemplateEventHandler: PostEmailTemplateEventHandler,
    private val transactionalTemplates: TransactionTemplates
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * `@EventListener`를 사용한 이유:
     * `EmailEventPublisher`에서 트랜잭션에 `TransactionSynchronization`를 등록하는 방식으로 이벤트 발행 시점을 제어하고 있다.
     * @see com.manage.crm.email.support.EmailEventPublisher.publishEvent
     */
    @Async
    @EventListener
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
