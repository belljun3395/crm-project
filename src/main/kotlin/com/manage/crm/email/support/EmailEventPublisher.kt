package com.manage.crm.email.support

import com.manage.crm.email.event.schedule.CancelScheduledEvent
import com.manage.crm.email.event.send.EmailSendEvent
import com.manage.crm.email.event.send.EmailSentEvent
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutEvent
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class EmailEventPublisher(
    val applicationEventPublisher: ApplicationEventPublisher
) {
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
}
