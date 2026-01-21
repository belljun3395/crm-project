package com.manage.crm.infrastructure.scheduler.consumer

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.service.ScheduleTaskServiceImpl
import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * Kafka consumer for scheduled task events
 * Processes messages from the scheduled-tasks topic and executes business logic
 */
@Component
class ScheduledTaskConsumer(
    private val scheduleTaskService: ScheduleTaskServiceImpl
) {
    private val log = KotlinLogging.logger {}

    @KafkaListener(
        topics = ["scheduled-tasks"],
        groupId = "crm-scheduled-tasks-consumer",
        containerFactory = "scheduledTaskKafkaListenerContainerFactory"
    )
    fun consume(event: ScheduledTaskEvent, acknowledgment: Acknowledgment) {
        runBlocking {
            try {
                log.info { "Received scheduled task event: ${event.scheduleName}" }

                when (val payload = event.payload) {
                    is NotificationEmailSendTimeOutEventInput -> {
                        processNotificationEmailTimeout(payload)
                    }
                    else -> {
                        log.warn { "Unknown schedule payload type: ${payload::class.simpleName}" }
                    }
                }

                // Manual acknowledgment after successful processing
                acknowledgment.acknowledge()
                log.info { "Successfully processed scheduled task: ${event.scheduleName}" }
            } catch (ex: Exception) {
                log.error(ex) { "Failed to process scheduled task: ${event.scheduleName}" }
                // Note: With manual acknowledgment, failed messages will be reprocessed
                // Consider implementing DLQ (Dead Letter Queue) for repeatedly failing messages
                throw ex
            }
        }
    }

    private suspend fun processNotificationEmailTimeout(input: NotificationEmailSendTimeOutEventInput) {
        log.info {
            "Processing notification email timeout: templateId=${input.templateId}, " +
                "userCount=${input.userIds.size}, eventId=${input.eventId}"
        }

        scheduleTaskService.processNotificationEmailTimeout(input)
    }

    /**
     * Add additional processing methods for different ScheduleInfo types here
     * Example:
     * private suspend fun processOtherScheduleType(input: OtherScheduleInfo) { ... }
     */
}
