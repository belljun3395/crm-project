package com.manage.crm.infrastructure.scheduler.service

import com.manage.crm.infrastructure.scheduler.executor.ScheduledTaskExecutor
import com.manage.crm.infrastructure.scheduler.provider.RedisSchedulerProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * Redis 기반 스케줄 모니터링 서비스
 * 주기적으로 Redis를 폴링하여 실행 시간이 된 작업들을 Kafka로 전송합니다.
 */
@Service
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class RedisScheduleMonitoringService(
    private val redisSchedulerProvider: RedisSchedulerProvider,
    private val scheduledTaskExecutor: ScheduledTaskExecutor
) {

    private val log = KotlinLogging.logger {}
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * 매 1초마다 만료된 스케줄을 확인하고 실행합니다.
     * fixedDelay를 사용하여 이전 작업이 완료된 후 1초 후에 다음 작업을 시작합니다.
     */
    @Scheduled(fixedDelay = 1000)
    fun processExpiredSchedules() {
        try {
            val expiredTasks = redisSchedulerProvider.getExpiredSchedules()

            if (expiredTasks.isEmpty()) {
                return
            }

            log.info { "Found ${expiredTasks.size} expired schedule(s) to process" }

            // 각 만료된 작업을 병렬로 처리
            expiredTasks.forEach { task ->
                coroutineScope.launch {
                    try {
                        // Kafka로 작업 실행 요청
                        scheduledTaskExecutor.executeScheduledTask(task.taskId, task.scheduleInfo)

                        // 성공적으로 전송된 작업은 Redis에서 제거
                        redisSchedulerProvider.removeSchedulesAtomically(listOf(task.taskId))

                        log.info { "Successfully processed and removed scheduled task: ${task.taskId}" }
                    } catch (ex: Exception) {
                        log.error(ex) { "Failed to process scheduled task: ${task.taskId}. Will retry in next cycle." }
                        // 실패한 작업은 Redis에 남겨두어 다음 사이클에서 재시도
                    }
                }
            }
        } catch (ex: Exception) {
            log.error(ex) { "Error during scheduled task processing cycle" }
        }
    }

    /**
     * 시스템 상태를 로깅합니다. (선택적)
     * 매 30초마다 현재 등록된 스케줄 수를 로깅합니다.
     */
    @Scheduled(fixedDelay = 30000)
    fun logSchedulerStatus() {
        try {
            val totalSchedules = redisSchedulerProvider.browseSchedule().size
            log.debug { "Redis scheduler status - Total schedules: $totalSchedules" }
        } catch (ex: Exception) {
            log.warn(ex) { "Failed to get scheduler status" }
        }
    }
}
