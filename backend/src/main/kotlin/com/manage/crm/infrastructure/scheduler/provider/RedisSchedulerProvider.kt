package com.manage.crm.infrastructure.scheduler.provider

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import com.manage.crm.infrastructure.scheduler.ScheduleName
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * `Redis` 기반 스케줄러 제공자
 * `Redis Sorted Set`을 사용하여 스케줄을 관리합니다.
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class RedisSchedulerProvider(
    private val redisSchedulerService: RedisSchedulerService,
    private val objectMapper: ObjectMapper
) : SchedulerProvider {
    private val log = KotlinLogging.logger {}

    override fun createSchedule(name: String, schedule: LocalDateTime, input: ScheduleInfo): String {
        try {
            val score = schedule.toEpochSecond(ZoneOffset.UTC).toDouble()
            val taskData = RedisScheduledTask(
                taskId = name,
                scheduleInfo = input,
                scheduledAt = schedule,
                createdAt = LocalDateTime.now()
            )

            redisSchedulerService.setTaskData(name, objectMapper.writeValueAsString(taskData))
            redisSchedulerService.addTaskToScheduledTasks(name, score)

            log.info { "Successfully created Redis schedule: $name at $schedule (score: $score)" }
            return name
        } catch (ex: Exception) {
            log.error(ex) { "Error creating Redis schedule: $name" }
            throw RuntimeException("Error creating Redis schedule: $name", ex)
        }
    }

    override fun browseSchedule(): List<ScheduleName> {
        return try {
            val allTasks = redisSchedulerService.browseAllTasksInScheduledTasks()
            allTasks.map { ScheduleName(it as String) }
        } catch (ex: Exception) {
            log.error(ex) { "Error browsing Redis schedules" }
            emptyList()
        }
    }

    override fun deleteSchedule(scheduleName: ScheduleName) {
        try {
            redisSchedulerService.removeTaskInScheduledTasks(scheduleName.value)
            redisSchedulerService.removeTaskData(scheduleName.value)

            log.info { "Successfully deleted Redis schedule: ${scheduleName.value}" }
        } catch (ex: Exception) {
            log.error(ex) { "Error deleting Redis schedule: ${scheduleName.value}" }
            throw RuntimeException("Error deleting Redis schedule: ${scheduleName.value}", ex)
        }
    }

    override fun getProviderType(): String = "redis-kafka"

    /**
     * 현재 시간보다 이전에 실행되어야 하는 스케줄들을 조회합니다.
     * 스케줄 모니터링 서비스에서 사용됩니다.
     */
    fun getExpiredSchedules(): List<RedisScheduledTask> {
        return try {
            val currentTime = System.currentTimeMillis() / 1000.0
            val expiredTaskIds = redisSchedulerService.browseDueTaskIdsInScheduledTasks(currentTime)

            expiredTaskIds.mapNotNull { taskId ->
                val taskDataJson = redisSchedulerService.getTaskData(taskId.toString())
                taskDataJson?.let {
                    try {
                        objectMapper.readValue(it, RedisScheduledTask::class.java)
                    } catch (ex: Exception) {
                        log.warn(ex) { "Failed to deserialize task data for $taskId, removing corrupted data" }
                        redisSchedulerService.removeTaskData(taskId.toString())
                        null
                    }
                }
            }
        } catch (ex: Exception) {
            log.error(ex) { "Error getting expired schedules" }
            emptyList()
        }
    }

    /**
     * 여러 스케줄을 일괄 제거합니다.
     * 단순하지만 안정적인 접근 방식을 사용합니다.
     */
    fun removeSchedulesAtomically(taskIds: List<String>): Long {
        if (taskIds.isEmpty()) return 0

        var removedCount = 0L

        taskIds.forEach { taskId ->
            try {
                val removedFromZSet = redisSchedulerService.removeTaskIdFromScheduledTasks(taskId)
                redisSchedulerService.removeTaskData(taskId)

                if (removedFromZSet > 0) {
                    removedCount++
                    log.debug { "Removed schedule: $taskId" }
                }
            } catch (ex: Exception) {
                log.warn(ex) { "Failed to remove schedule: $taskId" }
            }
        }

        log.debug { "Batch removed $removedCount schedules out of ${taskIds.size}" }
        return removedCount
    }
}

/**
 * `Redis`에 저장되는 스케줄 작업 정보
 * `Jackson`의 다형성 처리를 위해 타입 정보를 포함합니다.
 */
data class RedisScheduledTask(
    val taskId: String,
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class"
    )
    val scheduleInfo: ScheduleInfo,
    val scheduledAt: LocalDateTime,
    val createdAt: LocalDateTime
)
