package com.manage.crm.infrastructure.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.infrastructure.scheduler.executor.KafkaScheduledTaskExecutor
import com.manage.crm.infrastructure.scheduler.provider.RedisSchedulerProvider
import com.manage.crm.infrastructure.scheduler.provider.RedisSchedulerService
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
import org.springframework.data.redis.core.RedisTemplate
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
                val redisTemplate = mockk<RedisTemplate<String, Any>>()
                val kafkaTemplate = mockk<KafkaTemplate<String, Any>>()
                val objectMapper = ObjectMapper()

                val redisSchedulerService = RedisSchedulerService(redisTemplate, objectMapper)
                val redisSchedulerProvider = RedisSchedulerProvider(redisSchedulerService, objectMapper)
                val kafkaExecutor = KafkaScheduledTaskExecutor(kafkaTemplate)

                val processedTasks = AtomicInteger(0)
                val duplicateProcessAttempts = AtomicInteger(0)

                // Mock Redis atomic operation to return tasks only once
                val expiredTaskIds = setOf("task1", "task2", "task3")
                var atomicCallCount = 0

                every { 
                    redisTemplate.execute(
                        any<org.springframework.data.redis.core.script.RedisScript<List<*>>>(),
                        any<List<String>>(),
                        *anyVararg<String>()
                    )
                } answers {
                    atomicCallCount++
                    if (atomicCallCount == 1) {
                        expiredTaskIds.toList() // 첫 번째 호출에서만 작업 반환
                    } else {
                        emptyList<String>() // 이후 호출에서는 빈 집합 반환 (이미 제거됨)
                    }
                }

                // Mock task data retrieval
                every { redisTemplate.opsForValue().get(any<String>()) } returns """
                    {
                        "taskId": "test-task",
                        "scheduleInfo": {
                            "templateId": 1,
                            "templateVersion": 1.0,
                            "userIds": [1, 2, 3],
                            "eventId": {"value": "test-event"},
                            "expiredTime": "2025-01-01T12:00:00"
                        },
                        "scheduledAt": "2025-01-01T12:00:00",
                        "createdAt": "2025-01-01T11:00:00"
                    }
                """.trimIndent()

                every { redisTemplate.delete(any<String>()) } returns true

                // Mock successful Kafka sending
                val mockSendResult = mockk<SendResult<String, Any>>()
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
                        redisTemplate.execute(
                            any<org.springframework.data.redis.core.script.RedisScript<List<*>>>(),
                            any<List<String>>(),
                            *anyVararg<String>()
                        )
                    }

                    // 실제 처리된 작업 수는 3개여야 함 (중복 처리 없음)
                    processedTasks.get() shouldBe 3
                }
            }

            When("Kafka send fails and task needs to be rescheduled") {
                val redisTemplate = mockk<RedisTemplate<String, Any>>()
                val kafkaTemplate = mockk<KafkaTemplate<String, Any>>()
                val objectMapper = ObjectMapper()

                val redisSchedulerService = RedisSchedulerService(redisTemplate, objectMapper)
                val redisSchedulerProvider = RedisSchedulerProvider(redisSchedulerService, objectMapper)
                val kafkaExecutor = KafkaScheduledTaskExecutor(kafkaTemplate)
                val monitoringService = RedisScheduleMonitoringService(redisSchedulerProvider, kafkaExecutor)

                val rescheduleCount = AtomicInteger(0)

                // Mock atomic operation to return one failed task
                every { 
                    redisTemplate.execute(
                        any<org.springframework.data.redis.core.script.RedisScript<List<*>>>(),
                        any<List<String>>(),
                        *anyVararg<String>()
                    )
                } returns listOf("failed-task")

                // Mock task data
                every { redisTemplate.opsForValue().get(any<String>()) } returns """
                    {
                        "taskId": "failed-task",
                        "scheduleInfo": {
                            "templateId": 1,
                            "templateVersion": 1.0,
                            "userIds": [1],
                            "eventId": {"value": "failed-event"},
                            "expiredTime": "2025-01-01T12:00:00"
                        },
                        "scheduledAt": "2025-01-01T12:00:00",
                        "createdAt": "2025-01-01T11:00:00"
                    }
                """.trimIndent()

                every { redisTemplate.delete(any<String>()) } returns true

                // Mock Kafka failure
                every { kafkaTemplate.send(any<String>(), any<String>(), any()) } throws RuntimeException("Kafka unavailable")

                // Mock rescheduling
                every { redisTemplate.opsForZSet().add(any<String>(), any<String>(), any<Double>()) } answers {
                    rescheduleCount.incrementAndGet()
                    true
                }
                every { redisTemplate.opsForValue().set(any<String>(), any<String>()) } returns Unit

                Then("failed task should be rescheduled") {
                    monitoringService.processExpiredSchedules()

                    // 원자적 조회가 호출되었는지 확인
                    verify { 
                        redisTemplate.execute(
                            any<org.springframework.data.redis.core.script.RedisScript<List<*>>>(),
                            any<List<String>>(),
                            *anyVararg<String>()
                        )
                    }

                    // Kafka 전송 시도가 있었는지 확인
                    verify { kafkaTemplate.send(any<String>(), any<String>(), any()) }

                    // 재스케줄링이 발생했는지 확인
                    rescheduleCount.get() shouldBe 1
                    verify { redisTemplate.opsForZSet().add(any<String>(), any<String>(), any<Double>()) }
                }
            }
        }
    }
}
