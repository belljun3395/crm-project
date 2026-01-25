package com.manage.crm.email.event.schedule.handler

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import com.manage.crm.email.support.EmailEventPublisher
import org.springframework.stereotype.Component

@Component
class ScheduledTaskHandler(
    private val emailEventPublisher: EmailEventPublisher
) {
    fun handle(input: NotificationEmailSendTimeOutEventInput) {
        val event = NotificationEmailSendTimeOutInvokeEvent(
            timeOutEventId = input.eventId,
            templateId = input.templateId,
            templateVersion = input.templateVersion,
            userIds = input.userIds
        )
        emailEventPublisher.publishEvent(event)
    }
}
