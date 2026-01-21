package com.manage.crm.infrastructure.scheduler.provider

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import com.manage.crm.infrastructure.scheduler.ScheduleName
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Redis-based scheduler provider using Sorted Sets for time-based scheduling
 *
 * Data structure:
 * - Redis Sorted Set: crm:schedules
 * - Score: Unix timestamp (seconds since epoch)
 * - Value: JSON containing schedule name, schedule time, and payload
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class RedisSchedulerProvider(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : SchedulerProvider {
    private val log = KotlinLogging.logger {}

    companion object {
        private const val SCHEDULE_KEY = "crm:schedules"
        private const val SCHEDULE_METADATA_KEY_PREFIX = "crm:schedule:meta:"
        private const val ASIA_SEOUL_ZONE = "Asia/Seoul"
    }

    override suspend fun createSchedule(
        name: String,
        scheduleTime: LocalDateTime,
        input: ScheduleInfo
    ): ScheduleCreationResult {
        return try {
            val score = scheduleTime.atZone(ZoneId.of(ASIA_SEOUL_ZONE)).toEpochSecond().toDouble()
            val scheduleData = ScheduleData(
                name = name,
                scheduleTime = scheduleTime,
                payload = input
            )
            val jsonValue = objectMapper.writeValueAsString(scheduleData)

            // Store in sorted set with timestamp as score
            val added = redisTemplate.opsForZSet()
                .add(SCHEDULE_KEY, jsonValue, score)
                .awaitFirst()

            if (added) {
                // Store metadata for quick lookup
                redisTemplate.opsForValue()
                    .set("$SCHEDULE_METADATA_KEY_PREFIX$name", jsonValue)
                    .awaitFirstOrNull()

                log.info { "Successfully created Redis schedule: $name at $scheduleTime (score: $score)" }
                ScheduleCreationResult.Success(name)
            } else {
                log.warn { "Schedule $name already exists in Redis" }
                ScheduleCreationResult.Failure("Schedule already exists: $name")
            }
        } catch (ex: Exception) {
            log.error(ex) { "Failed to create Redis schedule: $name" }
            ScheduleCreationResult.Failure("Failed to create schedule: ${ex.message}", ex)
        }
    }

    override suspend fun browseSchedules(): List<ScheduleName> {
        return try {
            val schedules = redisTemplate.opsForZSet()
                .range(SCHEDULE_KEY, org.springframework.data.domain.Range.unbounded())
                .collectList()
                .awaitFirst()

            schedules.mapNotNull { json ->
                try {
                    val scheduleData = objectMapper.readValue(json, ScheduleData::class.java)
                    ScheduleName(scheduleData.name)
                } catch (ex: Exception) {
                    log.error(ex) { "Failed to parse schedule data: $json" }
                    null
                }
            }
        } catch (ex: Exception) {
            log.error(ex) { "Failed to browse Redis schedules" }
            emptyList()
        }
    }

    override suspend fun deleteSchedule(scheduleName: ScheduleName) {
        try {
            // Get metadata to find the exact value
            val metadata = redisTemplate.opsForValue()
                .get("$SCHEDULE_METADATA_KEY_PREFIX${scheduleName.value}")
                .awaitFirstOrNull()

            if (metadata != null) {
                // Remove from sorted set
                redisTemplate.opsForZSet()
                    .remove(SCHEDULE_KEY, metadata)
                    .awaitFirstOrNull()

                // Remove metadata
                redisTemplate.opsForValue()
                    .delete("$SCHEDULE_METADATA_KEY_PREFIX${scheduleName.value}")
                    .awaitFirstOrNull()

                log.info { "Successfully deleted Redis schedule: ${scheduleName.value}" }
            } else {
                log.warn { "Schedule not found in Redis: ${scheduleName.value}" }
            }
        } catch (ex: Exception) {
            log.error(ex) { "Failed to delete Redis schedule: ${scheduleName.value}" }
            throw RuntimeException("Failed to delete schedule: ${ex.message}", ex)
        }
    }

    /**
     * Fetches schedules that should be executed (score <= current timestamp)
     * This is called by the monitoring service
     */
    suspend fun fetchDueSchedules(): List<ScheduleData> {
        return try {
            val now = LocalDateTime.now(ZoneId.of(ASIA_SEOUL_ZONE))
                .toEpochSecond(ZoneOffset.of("+09:00"))
                .toDouble()

            val dueSchedules = redisTemplate.opsForZSet()
                .rangeByScore(SCHEDULE_KEY, org.springframework.data.domain.Range.closed(0.0, now))
                .collectList()
                .awaitFirst()

            dueSchedules.mapNotNull { json ->
                try {
                    objectMapper.readValue(json, ScheduleData::class.java)
                } catch (ex: Exception) {
                    log.error(ex) { "Failed to parse due schedule: $json" }
                    null
                }
            }
        } catch (ex: Exception) {
            log.error(ex) { "Failed to fetch due schedules from Redis" }
            emptyList()
        }
    }

    /**
     * Internal data structure for Redis storage
     */
    data class ScheduleData(
        val name: String,
        val scheduleTime: LocalDateTime,
        val payload: ScheduleInfo
    )
}
