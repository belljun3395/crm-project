package com.manage.crm.user.event

import com.manage.crm.support.coroutine.eventListenerCoroutineScope
import com.manage.crm.support.transactional.TransactionTemplates
import com.manage.crm.user.event.handler.NewUserEventHandler
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class UserTransactionEventListener(
    private val newUserEventHandler: NewUserEventHandler,
    private val transactionalTemplates: TransactionTemplates
) {
    /**
     * `@EventListener`를 사용한 이유:
     * `EmailEventPublisher`에서 트랜잭션에 `TransactionSynchronization`를 등록하는 방식으로 이벤트 발행 시점을 제어하고 있다.
     * @see com.manage.crm.user.support.UserEventPublisher
     */
    @EventListener
    fun handleAfterCompletionEvent(event: UserTransactionAfterCompletionEvent) {
        eventListenerCoroutineScope().apply {
            when (event) {
                is NewUserEvent -> launch {
                    transactionalTemplates.newTxWriter.executeAndAwait {
                        newUserEventHandler.handle(event)
                    }
                }
            }
        }
    }
}
