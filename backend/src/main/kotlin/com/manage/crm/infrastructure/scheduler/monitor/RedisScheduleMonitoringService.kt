package com.manage.crm.infrastructure.scheduler.monitor

import com.manage.crm.infrastructure.scheduler.executor.KafkaScheduledTaskExecutor
import com.manage.crm.infrastructure.scheduler.provider.RedisSchedulerProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

/**
 * Monitors Redis for due schedules and publishes them to Kafka.
 * Polls Redis every second to check for schedules that need to be executed.
 */
@Service
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class RedisScheduleMonitoringService(
    private val redisSchedulerProvider: RedisSchedulerProvider,
    private val kafkaExecutor: KafkaScheduledTaskExecutor
) {
    private val log = KotlinLogging.logger {}
    private val scope = CoroutineScope(Dispatchers.Default)
    private var monitoringJob: Job? = null

    companion object {
        const val POLLING_INTERVAL_MS = 1000L
    }

    @EventListener(ApplicationReadyEvent::class)
    fun startMonitoring() {
        log.info { "Starting Redis schedule monitoring service" }
        monitoringJob = scope.launch {
            processSchedules()
        }
    }

    @PreDestroy
    fun stopMonitoring() {
        log.info { "Stopping Redis schedule monitoring service" }
        monitoringJob?.cancel()
    }

    private suspend fun processSchedules() {
        while (scope.isActive) {
            try {
                val dueSchedules = redisSchedulerProvider.fetchDueSchedules()

                if (dueSchedules.isNotEmpty()) {
                    log.debug { "Found ${dueSchedules.size} due schedules" }

                    dueSchedules.forEach { event ->
                        try {
                            val result = kafkaExecutor.execute(event)
                            result.whenComplete { success, _ ->
                                if (success) {
                                    redisSchedulerProvider.removeProcessedSchedule(event.scheduleName)
                                }
                            }
                        } catch (ex: Exception) {
                            log.error(ex) { "Failed to process schedule: ${event.scheduleName}" }
                        }
                    }
                }
            } catch (ex: Exception) {
                log.error(ex) { "Error during schedule monitoring cycle" }
            }

            delay(POLLING_INTERVAL_MS)
        }
    }
}
