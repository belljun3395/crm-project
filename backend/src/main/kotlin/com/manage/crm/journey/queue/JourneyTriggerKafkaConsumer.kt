package com.manage.crm.journey.queue

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka", matchIfMissing = true)
class JourneyTriggerKafkaConsumer(
    private val processor: JourneyTriggerQueueProcessor,
) {
    private val log = KotlinLogging.logger {}

    @KafkaListener(
        topics = [JourneyTriggerQueuePublisher.TOPIC],
        groupId = "\${spring.kafka.consumer.journey-group-id:crm-journey-trigger-consumer}",
        containerFactory = "journeyTriggerKafkaListenerContainerFactory",
    )
    fun consume(
        message: JourneyTriggerQueueMessage,
        acknowledgment: Acknowledgment,
    ) {
        try {
            runBlocking {
                processor.process(message)
            }
        } catch (error: Exception) {
            log.error(error) { "Failed to process journey trigger message from Kafka: $message" }
        } finally {
            acknowledgment.acknowledge()
        }
    }
}
