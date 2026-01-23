package com.manage.crm.infrastructure.scheduler.provider

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.ScheduleName
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.data.domain.Range
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import org.springframework.data.redis.core.ReactiveZSetOperations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

class RedisSchedulerProviderTest {

    private lateinit var redisTemplate: ReactiveRedisTemplate<String, String>
    private lateinit var objectMapper: ObjectMapper
    private lateinit var redisSchedulerProvider: RedisSchedulerProvider
    private lateinit var zSetOps: ReactiveZSetOperations<String, String>
    private lateinit var valueOps: ReactiveValueOperations<String, String>

    @BeforeEach
    fun setUp() {
        redisTemplate = mock()
        zSetOps = mock()
        valueOps = mock()
        objectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())

        `when`(redisTemplate.opsForZSet()).thenReturn(zSetOps)
        `when`(redisTemplate.opsForValue()).thenReturn(valueOps)

        redisSchedulerProvider = RedisSchedulerProvider(redisTemplate, objectMapper)
    }

    @Test
    fun `createSchedule should add schedule to Redis sorted set`() = runTest {
        // given
        val name = "test-schedule-123"
        val scheduleTime = LocalDateTime.of(2024, 6, 1, 12, 0, 0)
        val input = NotificationEmailSendTimeOutEventInput(
            templateId = 1L,
            templateVersion = 1.0f,
            userIds = listOf(1L, 2L),
            eventId = EventId(name),
            expiredTime = scheduleTime
        )

        `when`(zSetOps.add(eq("crm:schedules"), any(), any())).thenReturn(Mono.just(true))
        `when`(valueOps.set(any(), any())).thenReturn(Mono.just(true))

        // when
        val result = redisSchedulerProvider.createSchedule(name, scheduleTime, input)

        // then
        assertTrue(result is ScheduleCreationResult.Success)
        assertEquals(name, (result as ScheduleCreationResult.Success).scheduleId)
    }

    @Test
    fun `createSchedule should return failure when schedule already exists`() = runTest {
        // given
        val name = "existing-schedule"
        val scheduleTime = LocalDateTime.of(2024, 6, 1, 12, 0, 0)
        val input = NotificationEmailSendTimeOutEventInput(
            templateId = 1L,
            templateVersion = 1.0f,
            userIds = listOf(1L),
            eventId = EventId(name),
            expiredTime = scheduleTime
        )

        `when`(zSetOps.add(eq("crm:schedules"), any(), any())).thenReturn(Mono.just(false))

        // when
        val result = redisSchedulerProvider.createSchedule(name, scheduleTime, input)

        // then
        assertTrue(result is ScheduleCreationResult.Failure)
        assertTrue((result as ScheduleCreationResult.Failure).reason.contains("already exists"))
    }

    @Test
    fun `browseSchedules should return list of schedule names`() = runTest {
        // given
        val scheduleData1 = RedisSchedulerProvider.ScheduleData(
            name = "schedule-1",
            scheduleTime = LocalDateTime.now(),
            payload = NotificationEmailSendTimeOutEventInput(
                templateId = 1L,
                templateVersion = 1.0f,
                userIds = listOf(1L),
                eventId = EventId("schedule-1"),
                expiredTime = LocalDateTime.now()
            )
        )
        val scheduleData2 = RedisSchedulerProvider.ScheduleData(
            name = "schedule-2",
            scheduleTime = LocalDateTime.now(),
            payload = NotificationEmailSendTimeOutEventInput(
                templateId = 2L,
                templateVersion = 1.0f,
                userIds = listOf(2L),
                eventId = EventId("schedule-2"),
                expiredTime = LocalDateTime.now()
            )
        )

        val jsonList = listOf(
            objectMapper.writeValueAsString(scheduleData1),
            objectMapper.writeValueAsString(scheduleData2)
        )

        `when`(zSetOps.range(eq("crm:schedules"), any<Range<Long>>())).thenReturn(Flux.fromIterable(jsonList))

        // when
        val result = redisSchedulerProvider.browseSchedules()

        // then
        assertEquals(2, result.size)
        assertEquals("schedule-1", result[0].value)
        assertEquals("schedule-2", result[1].value)
    }

    @Test
    fun `deleteSchedule should remove schedule from Redis`() = runTest {
        // given
        val scheduleName = ScheduleName("test-schedule")
        val metadata = """{"name":"test-schedule","scheduleTime":"2024-06-01T12:00:00","payload":{}}"""

        `when`(valueOps.get(eq("crm:schedule:meta:test-schedule"))).thenReturn(Mono.just(metadata))
        `when`(zSetOps.remove(eq("crm:schedules"), eq(metadata))).thenReturn(Mono.just(1L))
        `when`(valueOps.delete(eq("crm:schedule:meta:test-schedule"))).thenReturn(Mono.just(true))

        // when & then (no exception means success)
        redisSchedulerProvider.deleteSchedule(scheduleName)
    }

    @Test
    fun `fetchDueSchedules should return schedules with score less than current time`() = runTest {
        // given
        val pastTime = LocalDateTime.now().minusMinutes(5)
        val scheduleData = RedisSchedulerProvider.ScheduleData(
            name = "due-schedule",
            scheduleTime = pastTime,
            payload = NotificationEmailSendTimeOutEventInput(
                templateId = 1L,
                templateVersion = 1.0f,
                userIds = listOf(1L),
                eventId = EventId("due-schedule"),
                expiredTime = pastTime
            )
        )
        val json = objectMapper.writeValueAsString(scheduleData)

        `when`(zSetOps.rangeByScore(eq("crm:schedules"), any<Range<Double>>())).thenReturn(Flux.just(json))

        // when
        val result = redisSchedulerProvider.fetchDueSchedules()

        // then
        assertEquals(1, result.size)
        assertEquals("due-schedule", result[0].name)
    }
}
