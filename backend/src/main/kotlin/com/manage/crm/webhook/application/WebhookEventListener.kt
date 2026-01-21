package com.manage.crm.webhook.application

import com.manage.crm.email.event.send.EmailSentEvent
import com.manage.crm.support.coroutine.eventListenerCoroutineScope
import com.manage.crm.support.transactional.TransactionTemplates
import com.manage.crm.user.event.NewUserEvent
import com.manage.crm.webhook.domain.WebhookEventType
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class WebhookEventListener(
    private val webhookDispatchService: WebhookDispatchService,
    private val transactionalTemplates: TransactionTemplates
) {
    @EventListener
    fun onUserCreated(event: NewUserEvent) {
        eventListenerCoroutineScope().launch {
            transactionalTemplates.newTxWriter.executeAndAwait {
                webhookDispatchService.dispatch(
                    WebhookEventType.USER_CREATED,
                    mapOf("userId" to event.userId)
                )
            }
        }
    }

    @EventListener
    fun onEmailSent(event: EmailSentEvent) {
        eventListenerCoroutineScope().launch {
            transactionalTemplates.newTxWriter.executeAndAwait {
                webhookDispatchService.dispatch(
                    WebhookEventType.EMAIL_SENT,
                    mapOf(
                        "userId" to event.userId,
                        "messageId" to event.messageId,
                        "destination" to event.destination,
                        "timestamp" to event.timestamp.toString(),
                        "provider" to event.provider.name
                    )
                )
            }
        }
    }
}
