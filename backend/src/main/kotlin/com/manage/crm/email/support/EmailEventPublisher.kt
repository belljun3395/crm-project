package com.manage.crm.email.support

import arrow.fx.coroutines.parMap
import com.manage.crm.email.event.schedule.CancelScheduledEvent
import com.manage.crm.email.event.send.EmailSendEvent
import com.manage.crm.email.event.send.EmailSentEvent
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutEvent
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import com.manage.crm.email.event.template.PostEmailTemplateEvent
import com.manage.crm.support.transactional.TransactionSynchronizationTemplate
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class EmailEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val transactionSynchronizationTemplate: TransactionSynchronizationTemplate
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
            transactionSynchronizationTemplate.afterCommit(
                Dispatchers.IO + MDCContext(),
                blockDescription = "publish event: $event"
            ) {
                applicationEventPublisher.publishEvent(event)
                log.info { "published event: $event" }
            }
        }
    }
}
