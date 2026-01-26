package com.manage.crm.infrastructure.scheduler.provider

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import com.manage.crm.infrastructure.scheduler.ScheduleName
import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Redis Sorted Set based implementation of SchedulerProvider.
 * Uses Unix timestamps as scores for time-based ordering.
 *
 * Data structure:
 * - ZSET "crm:schedules" -> schedule names with timestamp scores
 * - STRING "crm:schedule:meta:{name}" -> JSON payload for each schedule
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class RedisSchedulerProvider(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : SchedulerProvider {
    private val log = KotlinLogging.logger {}

    companion object {
        const val SCHEDULE_ZSET_KEY = "crm:schedules"
        const val SCHEDULE_META_PREFIX = "crm:schedule:meta:"
        val TIMEZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }

    override suspend fun createSchedule(
        name: String,
        scheduleTime: LocalDateTime,
        input: ScheduleInfo
    ): ScheduleCreationResult {
        return try {
            val score = scheduleTime.atZone(TIMEZONE).toInstant().toEpochMilli().toDouble()
            val scheduleId = UUID.randomUUID().toString()

            val event = ScheduledTaskEvent(
                scheduleName = name,
                scheduleTime = scheduleTime,
                payload = input
            )
            val eventJson = objectMapper.writeValueAsString(event)

            // Store in sorted set with timestamp score
            redisTemplate.opsForZSet().add(SCHEDULE_ZSET_KEY, name, score)

            // Store metadata for quick lookup
            redisTemplate.opsForValue().set("$SCHEDULE_META_PREFIX$name", eventJson)

            log.info { "Created schedule: $name at $scheduleTime with ID: $scheduleId" }
            ScheduleCreationResult.Success(scheduleId)
        } catch (ex: Exception) {
            log.error(ex) { "Failed to create schedule: $name" }
            ScheduleCreationResult.Failure(ex.message ?: "Unknown error", ex)
        }
    }

    override suspend fun browseSchedules(): List<ScheduleName> {
        return try {
            val names = redisTemplate.opsForZSet().range(SCHEDULE_ZSET_KEY, 0, -1)
            names?.map { ScheduleName(it) } ?: emptyList()
        } catch (ex: Exception) {
            log.error(ex) { "Failed to browse schedules" }
            emptyList()
        }
    }

    override suspend fun deleteSchedule(scheduleName: ScheduleName) {
        try {
            redisTemplate.opsForZSet().remove(SCHEDULE_ZSET_KEY, scheduleName.value)
            redisTemplate.delete("$SCHEDULE_META_PREFIX${scheduleName.value}")
            log.info { "Deleted schedule: ${scheduleName.value}" }
        } catch (ex: Exception) {
            log.error(ex) { "Failed to delete schedule: ${scheduleName.value}" }
            throw RuntimeException("Failed to delete schedule: ${ex.message}", ex)
        }
    }

    /**
     * Fetches schedules that are due for execution (score <= current timestamp).
     * Used by the monitoring service.
     *
     * @return List of ScheduledTaskEvent that are due
     */
    fun fetchDueSchedules(): List<ScheduledTaskEvent> {
        val now = LocalDateTime.now().atZone(TIMEZONE).toInstant().toEpochMilli().toDouble()

        return try {
            val dueNames = redisTemplate.opsForZSet()
                .rangeByScore(SCHEDULE_ZSET_KEY, Double.MIN_VALUE, now)
                ?: emptySet()

            dueNames.mapNotNull { name ->
                val json = redisTemplate.opsForValue().get("$SCHEDULE_META_PREFIX$name")
                json?.let {
                    try {
                        objectMapper.readValue(it, ScheduledTaskEvent::class.java)
                    } catch (ex: Exception) {
                        log.error(ex) { "Failed to deserialize schedule event: $name" }
                        null
                    }
                }
            }
        } catch (ex: Exception) {
            log.error(ex) { "Failed to fetch due schedules" }
            emptyList()
        }
    }

    /**
     * Removes a schedule after successful processing.
     */
    fun removeProcessedSchedule(scheduleName: String) {
        try {
            redisTemplate.opsForZSet().remove(SCHEDULE_ZSET_KEY, scheduleName)
            redisTemplate.delete("$SCHEDULE_META_PREFIX$scheduleName")
            log.debug { "Removed processed schedule: $scheduleName" }
        } catch (ex: Exception) {
            log.error(ex) { "Failed to remove processed schedule: $scheduleName" }
        }
    }
}
