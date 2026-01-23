package com.manage.crm.infrastructure.scheduler.monitor

import com.manage.crm.infrastructure.scheduler.ScheduleName
import com.manage.crm.infrastructure.scheduler.executor.KafkaScheduledTaskExecutor
import com.manage.crm.infrastructure.scheduler.provider.RedisSchedulerProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.seconds

/**
 * Redis Schedule Monitoring Service
 * Periodically checks Redis for due schedules and publishes them to Kafka
 *
 * Polling interval: 1 second
 * Processing flow:
 * 1. Fetch schedules with score <= current timestamp
 * 2. Publish each schedule to Kafka
 * 3. Remove successfully published schedules from Redis
 */
@Service
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class RedisScheduleMonitoringService(
    private val redisSchedulerProvider: RedisSchedulerProvider,
    private val kafkaExecutor: KafkaScheduledTaskExecutor
) {
    private val log = KotlinLogging.logger {}
    private val monitoringScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private val POLLING_INTERVAL = 1.seconds
    }

    @EventListener(ApplicationReadyEvent::class)
    fun startMonitoring() {
        log.info { "Starting Redis Schedule Monitoring Service with ${POLLING_INTERVAL.inWholeSeconds}s interval" }

        monitoringScope.launch {
            while (true) {
                try {
                    processSchedules()
                } catch (ex: Exception) {
                    log.error(ex) { "Error in schedule monitoring loop" }
                }
                delay(POLLING_INTERVAL)
            }
        }
    }

    private suspend fun processSchedules() {
        val dueSchedules = redisSchedulerProvider.fetchDueSchedules()

        if (dueSchedules.isEmpty()) {
            return
        }

        log.info { "Found ${dueSchedules.size} due schedules to process" }

        dueSchedules.forEach { schedule ->
            try {
                // Publish to Kafka
                val success = kafkaExecutor.execute(schedule)

                if (success) {
                    // Remove from Redis after successful Kafka publish
                    redisSchedulerProvider.deleteSchedule(ScheduleName(schedule.name))
                    log.info { "Successfully processed and removed schedule: ${schedule.name}" }
                } else {
                    log.warn { "Failed to publish schedule to Kafka, will retry: ${schedule.name}" }
                }
            } catch (ex: Exception) {
                log.error(ex) { "Error processing schedule: ${schedule.name}" }
            }
        }
    }

    /**
     * Manual trigger for processing schedules (useful for testing)
     */
    suspend fun triggerProcessing() {
        processSchedules()
    }
}
