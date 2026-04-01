package com.manage.crm.journey.queue

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class KafkaJourneyTriggerQueuePublisher(
    private val journeyTriggerKafkaTemplate: KafkaTemplate<String, JourneyTriggerQueueMessage>,
) : JourneyTriggerQueuePublisher {
    private val log = KotlinLogging.logger {}

    override suspend fun publishEventTrigger(event: JourneyEventPayload) {
        val eventId = event.id
        val message =
            JourneyTriggerQueueMessage(
                triggerType = JourneyTriggerQueueType.EVENT,
                event = event,
            )

        val future = journeyTriggerKafkaTemplate.send(JourneyTriggerQueuePublisher.TOPIC, "event-$eventId", message)
        future.whenComplete { result: SendResult<String, JourneyTriggerQueueMessage>?, ex: Throwable? ->
            if (ex != null) {
                log.error(ex) { "Failed to publish EVENT journey trigger message to Kafka: eventId=$eventId" }
            } else {
                log.debug {
                    "Published EVENT journey trigger message to Kafka: eventId=$eventId, partition=${result?.recordMetadata?.partition()}, offset=${result?.recordMetadata?.offset()}"
                }
            }
        }
    }

    override suspend fun publishSegmentContextTrigger(changedUserIds: List<Long>) {
        val message =
            JourneyTriggerQueueMessage(
                triggerType = JourneyTriggerQueueType.SEGMENT_CONTEXT,
                changedUserIds = changedUserIds.ifEmpty { null },
            )
        val key = "segment-context-${changedUserIds.joinToString(",").ifEmpty { "all" }}"
        val future = journeyTriggerKafkaTemplate.send(JourneyTriggerQueuePublisher.TOPIC, key, message)
        future.whenComplete { result: SendResult<String, JourneyTriggerQueueMessage>?, ex: Throwable? ->
            if (ex != null) {
                log.error(ex) { "Failed to publish SEGMENT_CONTEXT journey trigger message to Kafka" }
            } else {
                log.debug {
                    "Published SEGMENT_CONTEXT journey trigger message to Kafka: partition=${result?.recordMetadata?.partition()}, offset=${result?.recordMetadata?.offset()}"
                }
            }
        }
    }
}
