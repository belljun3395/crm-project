package com.manage.crm.infrastructure.scheduler.consumer

import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import com.manage.crm.infrastructure.scheduler.handler.ScheduledTaskHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * Kafka consumer for scheduled task events.
 * Processes events and delegates to the appropriate handler.
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
    fun consume(event: ScheduledTaskEvent, ack: Acknowledgment) {
        log.info { "Received scheduled task: ${event.scheduleName}" }

        try {
            scheduledTaskHandler.handle(event)
            ack.acknowledge()
            log.debug { "Successfully processed scheduled task: ${event.scheduleName}" }
        } catch (ex: Exception) {
            log.error(ex) { "Failed to process scheduled task: ${event.scheduleName}" }
            // Don't acknowledge - message will be redelivered
            throw ex
        }
    }
}
