package com.manage.crm.infrastructure.scheduler.executor

import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import com.manage.crm.infrastructure.scheduler.provider.RedisSchedulerProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component

/**
 * Kafka-based scheduled task executor
 * Publishes scheduled task events to Kafka for distributed processing
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class KafkaScheduledTaskExecutor(
    private val kafkaTemplate: KafkaTemplate<String, ScheduledTaskEvent>
) {
    private val log = KotlinLogging.logger {}

    companion object {
        const val SCHEDULED_TASKS_TOPIC = "scheduled-tasks"
    }

    /**
     * Publishes a scheduled task event to Kafka
     * @param scheduleData Schedule data from Redis
     * @return true if successfully published, false otherwise
     */
    suspend fun execute(scheduleData: RedisSchedulerProvider.ScheduleData): Boolean {
        return try {
            val event = ScheduledTaskEvent(
                scheduleName = scheduleData.name,
                scheduleTime = scheduleData.scheduleTime,
                payload = scheduleData.payload
            )

            val future = kafkaTemplate.send(SCHEDULED_TASKS_TOPIC, scheduleData.name, event)

            future.whenComplete { result: SendResult<String, ScheduledTaskEvent>?, ex: Throwable? ->
                if (ex != null) {
                    log.error(ex) { "Failed to publish scheduled task to Kafka: ${scheduleData.name}" }
                } else {
                    log.info {
                        "Successfully published scheduled task to Kafka: ${scheduleData.name}, " +
                            "partition: ${result?.recordMetadata?.partition()}, " +
                            "offset: ${result?.recordMetadata?.offset()}"
                    }
                }
            }

            true
        } catch (ex: Exception) {
            log.error(ex) { "Exception while publishing scheduled task to Kafka: ${scheduleData.name}" }
            false
        }
    }

    /**
     * Publishes multiple scheduled tasks in batch
     */
    suspend fun executeBatch(schedules: List<RedisSchedulerProvider.ScheduleData>): Int {
        var successCount = 0
        schedules.forEach { schedule ->
            if (execute(schedule)) {
                successCount++
            }
        }
        return successCount
    }
}
