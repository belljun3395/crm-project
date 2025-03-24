package com.manage.crm.email.support

import arrow.fx.coroutines.parMap
import com.manage.crm.email.event.schedule.CancelScheduledEvent
import com.manage.crm.email.event.send.EmailSendEvent
import com.manage.crm.email.event.send.EmailSentEvent
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutEvent
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import com.manage.crm.email.event.template.PostEmailTemplateEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionSynchronization
import org.springframework.transaction.reactive.TransactionSynchronizationManager
import reactor.core.publisher.Mono

@Component
class EmailEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    val log = KotlinLogging.logger {}

    fun publishEvent(event: CancelScheduledEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    fun publishEvent(event: NotificationEmailSendTimeOutEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    fun publishEvent(event: EmailSendEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    fun publishEvent(event: NotificationEmailSendTimeOutInvokeEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    fun publishEvent(event: EmailSentEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    suspend fun publishEvent(events: List<PostEmailTemplateEvent>) {
        events.parMap { event ->
            TransactionSynchronizationManager.forCurrentTransaction().map { manager ->
                manager.registerSynchronization(object : TransactionSynchronization {
                    override fun afterCompletion(status: Int): Mono<Void> {
                        applicationEventPublisher.publishEvent(event)
                        log.info { "published event: $event" }
                        return super.afterCompletion(status)
                    }
                })
            }.awaitSingleOrNull()
        }
    }
}
