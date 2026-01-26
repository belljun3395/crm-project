package com.manage.crm.infrastructure.scheduler.provider

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.ScheduleName
import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.ZSetOperations
import java.time.LocalDateTime

class RedisSchedulerProviderTest : FeatureSpec({
    lateinit var redisTemplate: StringRedisTemplate
    lateinit var zSetOps: ZSetOperations<String, String>
    lateinit var valueOps: ValueOperations<String, String>
    lateinit var objectMapper: ObjectMapper
    lateinit var redisSchedulerProvider: RedisSchedulerProvider

    beforeTest {
        redisTemplate = mockk(relaxed = true)
        zSetOps = mockk(relaxed = true)
        valueOps = mockk(relaxed = true)
        objectMapper = jacksonObjectMapper().findAndRegisterModules()

        every { redisTemplate.opsForZSet() } returns zSetOps
        every { redisTemplate.opsForValue() } returns valueOps

        redisSchedulerProvider = RedisSchedulerProvider(redisTemplate, objectMapper)
    }

    feature("RedisSchedulerProvider#createSchedule") {
        scenario("should create schedule and return success result") {
            // given
            val name = "test-schedule-123"
            val scheduleTime = LocalDateTime.now().plusHours(1)
            val input = NotificationEmailSendTimeOutEventInput(
                templateId = 1L,
                templateVersion = 1.0f,
                userIds = listOf(1L, 2L),
                eventId = EventId("test-event-id"),
                expiredTime = scheduleTime
            )

            every { zSetOps.add(any(), any(), any()) } returns true
            every { valueOps.set(any(), any()) } returns Unit

            // when
            val result = runBlocking {
                redisSchedulerProvider.createSchedule(name, scheduleTime, input)
            }

            // then
            result.shouldBeInstanceOf<ScheduleCreationResult.Success>()

            verify(exactly = 1) { zSetOps.add(RedisSchedulerProvider.SCHEDULE_ZSET_KEY, name, any()) }
            verify(exactly = 1) { valueOps.set(eq("${RedisSchedulerProvider.SCHEDULE_META_PREFIX}$name"), any()) }
        }

        scenario("should return failure result on exception") {
            // given
            val name = "test-schedule-fail"
            val scheduleTime = LocalDateTime.now().plusHours(1)
            val input = NotificationEmailSendTimeOutEventInput(
                templateId = 1L,
                templateVersion = 1.0f,
                userIds = listOf(1L),
                eventId = EventId("test-event-id"),
                expiredTime = scheduleTime
            )

            every { zSetOps.add(any(), any(), any()) } throws RuntimeException("Redis connection failed")

            // when
            val result = runBlocking {
                redisSchedulerProvider.createSchedule(name, scheduleTime, input)
            }

            // then
            result.shouldBeInstanceOf<ScheduleCreationResult.Failure>()
            (result as ScheduleCreationResult.Failure).reason shouldBe "Redis connection failed"
        }
    }

    feature("RedisSchedulerProvider#browseSchedules") {
        scenario("should return list of schedule names") {
            // given
            val scheduleNames = setOf("schedule-1", "schedule-2", "schedule-3")
            every { zSetOps.range(RedisSchedulerProvider.SCHEDULE_ZSET_KEY, 0, -1) } returns scheduleNames

            // when
            val result = runBlocking {
                redisSchedulerProvider.browseSchedules()
            }

            // then
            result shouldHaveSize 3
            result.map { it.value } shouldBe listOf("schedule-1", "schedule-2", "schedule-3")
        }

        scenario("should return empty list when no schedules exist") {
            // given
            every { zSetOps.range(RedisSchedulerProvider.SCHEDULE_ZSET_KEY, 0, -1) } returns emptySet()

            // when
            val result = runBlocking {
                redisSchedulerProvider.browseSchedules()
            }

            // then
            result shouldHaveSize 0
        }
    }

    feature("RedisSchedulerProvider#deleteSchedule") {
        scenario("should delete schedule from redis") {
            // given
            val scheduleName = ScheduleName("schedule-to-delete")

            every { zSetOps.remove(any(), any()) } returns 1L
            every { redisTemplate.delete(any<String>()) } returns true

            // when
            runBlocking {
                redisSchedulerProvider.deleteSchedule(scheduleName)
            }

            // then
            verify(exactly = 1) { zSetOps.remove(RedisSchedulerProvider.SCHEDULE_ZSET_KEY, scheduleName.value) }
            verify(exactly = 1) { redisTemplate.delete("${RedisSchedulerProvider.SCHEDULE_META_PREFIX}${scheduleName.value}") }
        }
    }

    feature("RedisSchedulerProvider#fetchDueSchedules") {
        scenario("should return due schedules") {
            // given
            val dueScheduleNames = setOf("due-schedule-1", "due-schedule-2")
            every { zSetOps.rangeByScore(any(), any(), any()) } returns dueScheduleNames

            val scheduleTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
            val event1 = ScheduledTaskEvent(
                scheduleName = "due-schedule-1",
                scheduleTime = scheduleTime,
                payload = NotificationEmailSendTimeOutEventInput(
                    templateId = 1L,
                    templateVersion = 1.0f,
                    userIds = listOf(1L),
                    eventId = EventId("event-1"),
                    expiredTime = scheduleTime
                )
            )
            val event2 = ScheduledTaskEvent(
                scheduleName = "due-schedule-2",
                scheduleTime = scheduleTime,
                payload = NotificationEmailSendTimeOutEventInput(
                    templateId = 2L,
                    templateVersion = 1.0f,
                    userIds = listOf(2L),
                    eventId = EventId("event-2"),
                    expiredTime = scheduleTime
                )
            )

            val eventJson1 = objectMapper.writeValueAsString(event1)
            val eventJson2 = objectMapper.writeValueAsString(event2)

            every { valueOps.get("${RedisSchedulerProvider.SCHEDULE_META_PREFIX}due-schedule-1") } returns eventJson1
            every { valueOps.get("${RedisSchedulerProvider.SCHEDULE_META_PREFIX}due-schedule-2") } returns eventJson2

            // when
            val result = redisSchedulerProvider.fetchDueSchedules()

            // then
            result shouldHaveSize 2
            result[0].scheduleName shouldBe "due-schedule-1"
            result[1].scheduleName shouldBe "due-schedule-2"
        }

        scenario("should return empty list when no due schedules") {
            // given
            every { zSetOps.rangeByScore(any(), any(), any()) } returns emptySet()

            // when
            val result = redisSchedulerProvider.fetchDueSchedules()

            // then
            result shouldHaveSize 0
        }
    }
})
