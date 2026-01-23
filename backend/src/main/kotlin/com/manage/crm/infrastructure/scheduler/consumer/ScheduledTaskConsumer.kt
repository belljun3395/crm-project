package com.manage.crm.infrastructure.scheduler.consumer

import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import com.manage.crm.infrastructure.scheduler.handler.ScheduledTaskHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * Kafka consumer for scheduled task events
 * Processes messages from the scheduled-tasks topic and executes business logic
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class ScheduledTaskConsumer(
    private val scheduledTaskHandler: ScheduledTaskHandler
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

                scheduledTaskHandler.handle(event.payload)

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

    private fun processNotificationEmailTimeout(input: NotificationEmailSendTimeOutEventInput) {
        log.info {
            "Processing notification email timeout: templateId=${input.templateId}, " +
                "userCount=${input.userIds.size}, eventId=${input.eventId}"
        }

        // Publish event to trigger email sending via existing event handler
        applicationEventPublisher.publishEvent(
            NotificationEmailSendTimeOutInvokeEvent(
                timeOutEventId = input.eventId,
                templateId = input.templateId,
                templateVersion = input.templateVersion,
                userIds = input.userIds
            )
        )
    }

    /**
     * Add additional processing methods for different ScheduleInfo types here
     * Example:
     * private suspend fun processOtherScheduleType(input: OtherScheduleInfo) { ... }
     */
}
