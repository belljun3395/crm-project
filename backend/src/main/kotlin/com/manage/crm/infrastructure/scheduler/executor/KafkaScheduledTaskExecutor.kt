package com.manage.crm.infrastructure.scheduler.executor

import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

/**
 * Publishes scheduled task events to Kafka for distributed processing.
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class KafkaScheduledTaskExecutor(
    @Qualifier("scheduledTaskKafkaTemplate")
    private val kafkaTemplate: KafkaTemplate<String, ScheduledTaskEvent>
) {
    private val log = KotlinLogging.logger {}

    companion object {
        const val TOPIC = "scheduled-tasks"
    }

    /**
     * Publishes a single scheduled task event to Kafka.
     *
     * @param event The scheduled task event to publish
     * @return CompletableFuture for async result handling
     */
    fun execute(event: ScheduledTaskEvent): CompletableFuture<Boolean> {
        return try {
            val future = kafkaTemplate.send(TOPIC, event.scheduleName, event)

            CompletableFuture<Boolean>().also { result ->
                future.whenComplete { sendResult, ex ->
                    if (ex != null) {
                        log.error(ex) { "Failed to publish scheduled task: ${event.scheduleName}" }
                        result.complete(false)
                    } else {
                        log.info {
                            "Published scheduled task: ${event.scheduleName} to partition ${sendResult.recordMetadata.partition()}"
                        }
                        result.complete(true)
                    }
                }
            }
        } catch (ex: Exception) {
            log.error(ex) { "Error publishing scheduled task: ${event.scheduleName}" }
            CompletableFuture.completedFuture(false)
        }
    }

    /**
     * Publishes multiple scheduled task events to Kafka.
     *
     * @param events List of scheduled task events to publish
     * @return List of CompletableFutures for async result handling
     */
    fun executeBatch(events: List<ScheduledTaskEvent>): List<CompletableFuture<Boolean>> {
        return events.map { execute(it) }
    }
}
