package com.manage.crm.infrastructure.scheduler

import com.manage.crm.infrastructure.scheduler.executor.KafkaScheduledTaskExecutor
import com.manage.crm.infrastructure.scheduler.executor.ScheduledTaskMessage
import com.manage.crm.infrastructure.scheduler.provider.RedisScheduledTask
import com.manage.crm.infrastructure.scheduler.provider.RedisSchedulerProvider
import com.manage.crm.infrastructure.scheduler.service.RedisScheduleMonitoringService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

/**
 * 중복 처리 방지 기능을 테스트하는 통합 테스트
 */
@SpringBootTest
@TestPropertySource(properties = ["scheduler.provider=redis-kafka"])
class DuplicateProcessingPreventionIntegrationTest : BehaviorSpec() {

    init {
        Given("A Redis scheduler with atomic operations") {

            When("multiple monitoring services try to process the same expired tasks") {
                val redisSchedulerProvider = mockk<RedisSchedulerProvider>()
                val kafkaTemplate = mockk<KafkaTemplate<String, ScheduledTaskMessage>>()

                val kafkaExecutor = KafkaScheduledTaskExecutor(kafkaTemplate)

                val processedTasks = AtomicInteger(0)

                // Create test task objects
                val task1 = RedisScheduledTask(
                    taskId = "task1",
                    scheduleInfo = com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput(
                        templateId = 1L,
                        templateVersion = 1.0f,
                        userIds = listOf(1L, 2L, 3L),
                        eventId = com.manage.crm.email.domain.vo.EventId("test-event-1"),
                        expiredTime = java.time.LocalDateTime.of(2025, 1, 1, 12, 0, 0)
                    ),
                    scheduledAt = java.time.LocalDateTime.of(2025, 1, 1, 12, 0, 0),
                    createdAt = java.time.LocalDateTime.of(2025, 1, 1, 11, 0, 0)
                )

                val task2 = RedisScheduledTask(
                    taskId = "task2",
                    scheduleInfo = com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput(
                        templateId = 2L,
                        templateVersion = 1.0f,
                        userIds = listOf(4L, 5L, 6L),
                        eventId = com.manage.crm.email.domain.vo.EventId("test-event-2"),
                        expiredTime = java.time.LocalDateTime.of(2025, 1, 1, 12, 0, 0)
                    ),
                    scheduledAt = java.time.LocalDateTime.of(2025, 1, 1, 12, 0, 0),
                    createdAt = java.time.LocalDateTime.of(2025, 1, 1, 11, 0, 0)
                )

                val task3 = RedisScheduledTask(
                    taskId = "task3",
                    scheduleInfo = com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput(
                        templateId = 3L,
                        templateVersion = 1.0f,
                        userIds = listOf(7L, 8L, 9L),
                        eventId = com.manage.crm.email.domain.vo.EventId("test-event-3"),
                        expiredTime = java.time.LocalDateTime.of(2025, 1, 1, 12, 0, 0)
                    ),
                    scheduledAt = java.time.LocalDateTime.of(2025, 1, 1, 12, 0, 0),
                    createdAt = java.time.LocalDateTime.of(2025, 1, 1, 11, 0, 0)
                )

                val expiredTasks = listOf(task1, task2, task3)
                var atomicCallCount = 0

                every {
                    redisSchedulerProvider.getAndRemoveExpiredSchedules()
                } answers {
                    atomicCallCount++
                    if (atomicCallCount == 1) {
                        expiredTasks // 첫 번째 호출에서만 작업 반환
                    } else {
                        emptyList() // 이후 호출에서는 빈 리스트 반환 (이미 제거됨)
                    }
                }

                // Mock successful Kafka sending
                val mockSendResult = mockk<SendResult<String, ScheduledTaskMessage>>()
                val mockRecordMetadata = mockk<org.apache.kafka.clients.producer.RecordMetadata>()
                every { mockSendResult.recordMetadata } returns mockRecordMetadata
                every { mockRecordMetadata.offset() } returns 123L

                every { kafkaTemplate.send(any<String>(), any<String>(), any()) } answers {
                    processedTasks.incrementAndGet()
                    CompletableFuture.completedFuture(mockSendResult)
                }

                Then("only one monitoring service should process each task") {
                    runBlocking {
                        // 두 개의 모니터링 서비스 인스턴스 생성
                        val monitoringService1 = RedisScheduleMonitoringService(redisSchedulerProvider, kafkaExecutor)
                        val monitoringService2 = RedisScheduleMonitoringService(redisSchedulerProvider, kafkaExecutor)

                        // 동시에 실행
                        val job1 = async { monitoringService1.processExpiredSchedules() }
                        val job2 = async { monitoringService2.processExpiredSchedules() }

                        // 작업 완료 대기
                        job1.await()
                        job2.await()

                        // 작업이 완료될 때까지 대기
                        delay(1000)
                    }

                    // 원자적 연산이 정확히 2번 호출되었는지 확인 (각 서비스마다 1번씩)
                    verify(exactly = 2) {
                        redisSchedulerProvider.getAndRemoveExpiredSchedules()
                    }

                    // 실제 처리된 작업 수는 3개여야 함 (중복 처리 없음)
                    processedTasks.get() shouldBe 3
                }
            }

            When("Kafka send fails and task needs to be rescheduled") {
                val redisSchedulerProvider = mockk<RedisSchedulerProvider>()
                val kafkaTemplate = mockk<KafkaTemplate<String, ScheduledTaskMessage>>()

                val kafkaExecutor = KafkaScheduledTaskExecutor(kafkaTemplate)
                val monitoringService = RedisScheduleMonitoringService(redisSchedulerProvider, kafkaExecutor)

                val rescheduleCount = AtomicInteger(0)

                // Create failed task
                val failedTask = RedisScheduledTask(
                    taskId = "failed-task",
                    scheduleInfo = com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput(
                        templateId = 1L,
                        templateVersion = 1.0f,
                        userIds = listOf(1L),
                        eventId = com.manage.crm.email.domain.vo.EventId("failed-event"),
                        expiredTime = java.time.LocalDateTime.of(2025, 1, 1, 12, 0, 0)
                    ),
                    scheduledAt = java.time.LocalDateTime.of(2025, 1, 1, 12, 0, 0),
                    createdAt = java.time.LocalDateTime.of(2025, 1, 1, 11, 0, 0)
                )

                // Mock atomic operation to return one failed task
                every {
                    redisSchedulerProvider.getAndRemoveExpiredSchedules()
                } returns listOf(failedTask)

                // Mock Kafka failure
                every { kafkaTemplate.send(any<String>(), any<String>(), any()) } throws RuntimeException("Kafka unavailable")

                // Mock rescheduling
                every { redisSchedulerProvider.createSchedule(any<String>(), any<java.time.LocalDateTime>(), any()) } answers {
                    rescheduleCount.incrementAndGet()
                    "rescheduled-task-${System.currentTimeMillis()}"
                }

                Then("failed task should be rescheduled") {
                    runBlocking {
                        monitoringService.processExpiredSchedules()

                        // 비동기 작업 완료까지 대기
                        delay(2000)
                    }

                    // 원자적 조회가 호출되었는지 확인
                    verify {
                        redisSchedulerProvider.getAndRemoveExpiredSchedules()
                    }

                    // Kafka 전송 시도가 있었는지 확인
                    verify { kafkaTemplate.send(any<String>(), any<String>(), any()) }

                    // 재스케줄링이 발생했는지 확인
                    rescheduleCount.get() shouldBe 1
                    verify { redisSchedulerProvider.createSchedule(any<String>(), any<java.time.LocalDateTime>(), any()) }
                }
            }
        }
    }
}
