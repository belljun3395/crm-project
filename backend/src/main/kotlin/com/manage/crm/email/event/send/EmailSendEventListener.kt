package com.manage.crm.email.event.send

import com.manage.crm.email.event.send.handler.EmailClickEventHandler
import com.manage.crm.email.event.send.handler.EmailDeliveryDelayEventHandler
import com.manage.crm.email.event.send.handler.EmailDeliveryEventHandler
import com.manage.crm.email.event.send.handler.EmailOpenEventHandler
import com.manage.crm.email.event.send.handler.EmailSentEventHandler
import com.manage.crm.support.coroutine.eventListenerCoroutineScope
import com.manage.crm.support.transactional.TransactionTemplates
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.executeAndAwait

@Component
class EmailSendEventListener(
    private val emailSentEventHandler: EmailSentEventHandler,
    private val emailDeliveryEventHandler: EmailDeliveryEventHandler,
    private val emailOpenEventHandler: EmailOpenEventHandler,
    private val emailClickEventHandler: EmailClickEventHandler,
    private val emailDeliveryDelayEventHandler: EmailDeliveryDelayEventHandler,
    private val transactionalTemplates: TransactionTemplates
) {
    val log = KotlinLogging.logger {}

    @EventListener
    fun onEvent(event: EmailSendEvent) {
        eventListenerCoroutineScope().apply {
            when (event) {
                is EmailSentEvent -> launch {
                    transactionalTemplates.newTxWriter.executeAndAwait {
                        emailSentEventHandler.handle(event)
                    }
                }
                is EmailDeliveryEvent -> launch {
                    transactionalTemplates.newTxWriter.executeAndAwait {
                        emailDeliveryEventHandler.handle(event)
                    }
                }
                is EmailOpenEvent -> launch {
                    transactionalTemplates.newTxWriter.executeAndAwait {
                        emailOpenEventHandler.handle(event)
                    }
                }
                is EmailClickEvent -> launch {
                    transactionalTemplates.newTxWriter.executeAndAwait {
                        emailClickEventHandler.handle(event)
                    }
                }
                is EmailDeliveryDelayEvent -> launch {
                    transactionalTemplates.newTxWriter.executeAndAwait {
                        emailDeliveryDelayEventHandler.handle(event)
                    }
                }
            }
        }
    }
}
