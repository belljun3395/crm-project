package com.manage.crm.infrastructure.scheduler.provider

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import com.manage.crm.infrastructure.scheduler.ScheduleName
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Redis 기반 스케줄러 제공자
 * Redis Sorted Set을 사용하여 스케줄을 관리합니다.
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class RedisSchedulerProvider(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) : SchedulerProvider {

    private val log = KotlinLogging.logger {}

    companion object {
        private const val SCHEDULED_TASKS_KEY = "scheduled:tasks"
        private const val TASK_DATA_PREFIX = "scheduled:task:"
    }

    override fun createSchedule(name: String, schedule: LocalDateTime, input: ScheduleInfo): String {
        try {
            val score = schedule.toEpochSecond(ZoneOffset.UTC).toDouble()
            val taskDataKey = "$TASK_DATA_PREFIX$name"
            val taskData = RedisScheduledTask(
                taskId = name,
                scheduleInfo = input,
                scheduledAt = schedule,
                createdAt = LocalDateTime.now()
            )

            // 1. 작업 데이터를 별도 키에 저장
            redisTemplate.opsForValue().set(taskDataKey, objectMapper.writeValueAsString(taskData))

            // 2. Sorted Set에 실행 시간과 작업 ID 저장
            redisTemplate.opsForZSet().add(SCHEDULED_TASKS_KEY, name, score)

            log.info { "Successfully created Redis schedule: $name at $schedule (score: $score)" }
            return name
        } catch (ex: Exception) {
            log.error(ex) { "Error creating Redis schedule: $name" }
            throw RuntimeException("Error creating Redis schedule: $name", ex)
        }
    }

    override fun browseSchedule(): List<ScheduleName> {
        return try {
            val allTasks = redisTemplate.opsForZSet().range(SCHEDULED_TASKS_KEY, 0, -1)
                ?: emptySet()

            allTasks.map { ScheduleName(it as String) }
        } catch (ex: Exception) {
            log.error(ex) { "Error browsing Redis schedules" }
            emptyList()
        }
    }

    override fun deleteSchedule(scheduleName: ScheduleName) {
        try {
            val taskDataKey = "$TASK_DATA_PREFIX${scheduleName.value}"

            // 1. Sorted Set에서 제거
            redisTemplate.opsForZSet().remove(SCHEDULED_TASKS_KEY, scheduleName.value)

            // 2. 작업 데이터 삭제
            redisTemplate.delete(taskDataKey)

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
            val expiredTaskIds = redisTemplate.opsForZSet()
                .rangeByScore(SCHEDULED_TASKS_KEY, 0.0, currentTime) ?: emptySet()

            expiredTaskIds.mapNotNull { taskId ->
                val taskDataKey = "$TASK_DATA_PREFIX$taskId"
                val taskDataJson = redisTemplate.opsForValue().get(taskDataKey) as? String
                taskDataJson?.let { 
                    try {
                        objectMapper.readValue(it, RedisScheduledTask::class.java)
                    } catch (ex: Exception) {
                        log.warn(ex) { "Failed to deserialize task data for $taskId, removing corrupted data" }
                        redisTemplate.delete(taskDataKey)
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
                val taskDataKey = "$TASK_DATA_PREFIX$taskId"
                
                // 1. Sorted Set에서 제거
                val removedFromZSet = redisTemplate.opsForZSet().remove(SCHEDULED_TASKS_KEY, taskId)
                
                // 2. 작업 데이터 삭제 (ZSet에서 제거되었는지와 관계없이 항상 시도)
                redisTemplate.delete(taskDataKey)
                
                if (removedFromZSet != null && removedFromZSet > 0) {
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
 * Redis에 저장되는 스케줄 작업 정보
 * Jackson의 다형성 처리를 위해 타입 정보를 포함합니다.
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
