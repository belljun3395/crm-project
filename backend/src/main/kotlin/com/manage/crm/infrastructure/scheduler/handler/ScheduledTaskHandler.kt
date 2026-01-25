package com.manage.crm.infrastructure.scheduler.handler

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.event.send.notification.NotificationEmailSendTimeOutInvokeEvent
import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ScheduledTaskHandler(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    private val log = KotlinLogging.logger {}

    fun handle(payload: ScheduleInfo) {
        when (payload) {
            is NotificationEmailSendTimeOutEventInput -> {
                processNotificationEmailTimeout(payload)
            }
            else -> {
                log.warn { "Unknown schedule payload type: ${payload::class.simpleName}" }
            }
        }
    }

    private fun processNotificationEmailTimeout(input: NotificationEmailSendTimeOutEventInput) {
        log.info {
            "Processing notification email timeout: templateId=${input.templateId}, " +
                "userCount=${input.userIds.size}, eventId=${input.eventId}"
        }

        applicationEventPublisher.publishEvent(
            NotificationEmailSendTimeOutInvokeEvent(
                timeOutEventId = input.eventId,
                templateId = input.templateId,
                templateVersion = input.templateVersion,
                userIds = input.userIds
            )
        )
    }
}
