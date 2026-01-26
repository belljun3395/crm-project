package com.manage.crm.infrastructure.scheduler.handler

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Handles scheduled task events by routing to appropriate business logic.
 */
@Component
class ScheduledTaskHandler(
    private val eventPublisher: ApplicationEventPublisher
) {
    private val log = KotlinLogging.logger {}

    fun handle(event: ScheduledTaskEvent) {
        log.info { "Handling scheduled task: ${event.scheduleName}, type: ${event.payload::class.simpleName}" }

        when (val payload = event.payload) {
            is NotificationEmailSendTimeOutEventInput -> {
                processNotificationEmailTimeout(payload)
            }
            else -> {
                log.warn { "Unknown payload type: ${payload::class.simpleName}" }
            }
        }
    }

    private fun processNotificationEmailTimeout(input: NotificationEmailSendTimeOutEventInput) {
        log.info { "Processing notification email timeout: eventId=${input.eventId}, templateId=${input.templateId}" }

        val invokeEvent = NotificationEmailSendTimeOutInvokeEvent(
            timeOutEventId = input.eventId,
            templateId = input.templateId,
            templateVersion = input.templateVersion,
            userIds = input.userIds
        )

        eventPublisher.publishEvent(invokeEvent)
        log.debug { "Published NotificationEmailSendTimeOutInvokeEvent for eventId=${input.eventId}" }
    }
}
