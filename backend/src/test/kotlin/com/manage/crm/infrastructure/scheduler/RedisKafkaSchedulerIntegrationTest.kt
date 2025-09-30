package com.manage.crm.infrastructure.scheduler

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.application.service.ScheduleTaskServiceImpl
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.provider.RedisSchedulerProvider
import com.manage.crm.integration.AbstractIntegrationTest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.*

/**
 * Redis+Kafka 스케줄러 통합 테스트
 * 실제 Redis와 Kafka를 사용하여 전체 플로우를 테스트합니다.
 */
@SpringBootTest
@ActiveProfiles("test", "redis-kafka")
class RedisKafkaSchedulerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var scheduleTaskService: ScheduleTaskServiceImpl

    @Autowired
    private lateinit var redisSchedulerProvider: RedisSchedulerProvider

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @Test
    fun `should create and manage schedules using Redis+Kafka scheduler`() {
        runBlocking {
            // Given: 스케줄 입력 데이터 준비
            val futureTime = LocalDateTime.now().plusMinutes(1)
            val eventId = EventId(UUID.randomUUID().toString())
            val input = NotificationEmailSendTimeOutEventInput(
                templateId = 100L,
                templateVersion = 1.0f,
                userIds = listOf(1L, 2L, 3L),
                eventId = eventId,
                expiredTime = futureTime
            )

            // When: 새로운 스케줄 생성
            val scheduleId = scheduleTaskService.newSchedule(input)

            // Then: 스케줄이 Redis에 저장되었는지 확인
            scheduleId shouldBe eventId.value

            // Redis에서 직접 확인
            val allSchedules = redisSchedulerProvider.browseSchedule()
            allSchedules.any { it.value == eventId.value } shouldBe true

            // 스케줄 목록 조회 확인
            val scheduleViews = scheduleTaskService.browseScheduledTasksView()
            // 실제 데이터베이스에 이벤트가 저장되어야 조회되므로, 이 테스트에서는 스킵하거나 별도 설정 필요

            // When: 스케줄 취소
            scheduleTaskService.cancel(eventId.value)

            // Then: 스케줄이 Redis에서 제거되었는지 확인
            val schedulesAfterCancel = redisSchedulerProvider.browseSchedule()
            schedulesAfterCancel.none { it.value == eventId.value } shouldBe true
        }

        @Test
        fun `should handle schedule rescheduling`(): Unit = runBlocking {
            // Given: 원본 스케줄 생성
            val originalTime = LocalDateTime.now().plusMinutes(5)
            val eventId = EventId(UUID.randomUUID().toString())
            val originalInput = NotificationEmailSendTimeOutEventInput(
                templateId = 200L,
                templateVersion = 1.0f,
                userIds = listOf(10L, 20L),
                eventId = eventId,
                expiredTime = originalTime
            )

            scheduleTaskService.newSchedule(originalInput)

            // 원본 스케줄 존재 확인
            val initialSchedules = redisSchedulerProvider.browseSchedule()
            initialSchedules.any { it.value == eventId.value } shouldBe true

            // When: 스케줄 재설정 (시간 변경)
            val newTime = LocalDateTime.now().plusMinutes(10)
            val updatedInput = originalInput.copy(expiredTime = newTime)
            scheduleTaskService.reSchedule(updatedInput)

            // Then: 여전히 하나의 스케줄만 존재해야 함
            val finalSchedules = redisSchedulerProvider.browseSchedule()
            finalSchedules.count { it.value == eventId.value } shouldBe 1

            // 정리
            scheduleTaskService.cancel(eventId.value)
        }

        @Test
        fun `should process expired schedules automatically`() {
            runBlocking {
                // Given: 이미 만료된 시간으로 스케줄 생성
                val expiredTime = LocalDateTime.now().minusSeconds(30)
                val eventId = EventId(UUID.randomUUID().toString())
                val input = NotificationEmailSendTimeOutEventInput(
                    templateId = 300L,
                    templateVersion = 1.0f,
                    userIds = listOf(100L),
                    eventId = eventId,
                    expiredTime = expiredTime
                )

                // Redis에 직접 만료된 스케줄 생성
                redisSchedulerProvider.createSchedule(eventId.value, expiredTime, input)

                // 스케줄이 생성되었는지 확인
                val schedulesBeforeProcessing = redisSchedulerProvider.browseSchedule()
                schedulesBeforeProcessing.any { it.value == eventId.value } shouldBe true

                // When: 만료된 스케줄 처리 대기 (모니터링 서비스가 자동으로 처리)
                // 실제 환경에서는 1초마다 폴링하지만, 테스트에서는 수동으로 확인
                delay(2000) // 2초 대기

                // Then: 만료된 스케줄이 Redis에서 제거되었는지 확인
                val schedulesAfterProcessing = redisSchedulerProvider.browseSchedule()
                schedulesAfterProcessing.none { it.value == eventId.value } shouldBe true

                // Note: 실제 Kafka 메시지 처리까지 확인하려면 추가적인 Consumer 테스트나
                // TestContainers를 이용한 Kafka 통합 테스트가 필요합니다.
            }
        }

        @Test
        fun `should handle multiple concurrent schedules`(): Unit = runBlocking {
            // Given: 여러 개의 스케줄 동시 생성
            val scheduleCount = 10
            val eventIds = mutableListOf<EventId>()

            repeat(scheduleCount) { index ->
                val eventId = EventId(UUID.randomUUID().toString())
                eventIds.add(eventId)

                val input = NotificationEmailSendTimeOutEventInput(
                    templateId = (400L + index),
                    templateVersion = 1.0f,
                    userIds = listOf((1000L + index)),
                    eventId = eventId,
                    expiredTime = LocalDateTime.now().plusMinutes(index + 1L)
                )

                scheduleTaskService.newSchedule(input)
            }

            // When: 모든 스케줄이 생성되었는지 확인
            val allSchedules = redisSchedulerProvider.browseSchedule()

            // Then: 모든 스케줄이 존재해야 함
            eventIds.forEach { eventId ->
                allSchedules.any { it.value == eventId.value } shouldBe true
            }

            // 정리: 모든 스케줄 제거
            eventIds.forEach { eventId ->
                scheduleTaskService.cancel(eventId.value)
            }

            // 정리 확인
            val finalSchedules = redisSchedulerProvider.browseSchedule()
            eventIds.forEach { eventId ->
                finalSchedules.none { it.value == eventId.value } shouldBe true
            }
        }
    }
}
