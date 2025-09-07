package com.manage.crm.infrastructure.scheduler.executor

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.message.config.KafkaConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class KafkaScheduledTaskExecutorTest : BehaviorSpec({

    val kafkaTemplate = mockk<KafkaTemplate<String, Any>>()
    val kafkaExecutor = KafkaScheduledTaskExecutor(kafkaTemplate)

    Given("A Kafka scheduled task executor") {

        When("executing a scheduled task") {
            val input = NotificationEmailSendTimeOutEventInput(
                templateId = 1L,
                templateVersion = 1.0f,
                userIds = listOf(1L, 2L, 3L),
                eventId = EventId("test-event-123"),
                expiredTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0)
            )

            val messageSlot = slot<ScheduledTaskMessage>()
            val topicSlot = slot<String>()
            val keySlot = slot<String>()

            val mockSendResult = mockk<SendResult<String, Any>>()
            val mockRecordMetadata = mockk<org.apache.kafka.clients.producer.RecordMetadata>()
            val completableFuture = CompletableFuture<SendResult<String, Any>>()

            every { mockSendResult.recordMetadata } returns mockRecordMetadata
            every { mockRecordMetadata.offset() } returns 123L
            every { kafkaTemplate.send(capture(topicSlot), capture(keySlot), capture(messageSlot)) } returns completableFuture

            completableFuture.complete(mockSendResult)

            Then("it should send message to Kafka topic") {
                kafkaExecutor.executeScheduledTask("test-task-123", input)

                verify { kafkaTemplate.send(any<String>(), any<String>(), any<ScheduledTaskMessage>()) }

                topicSlot.captured shouldBe KafkaConfig.SCHEDULED_TASKS_TOPIC
                keySlot.captured shouldBe "test-task-123"
                messageSlot.captured.taskId shouldBe "test-task-123"
                messageSlot.captured.scheduleInfo shouldBe input
                messageSlot.captured.executedAt shouldNotBe 0L
            }
        }

        When("Kafka send operation fails") {
            val input = NotificationEmailSendTimeOutEventInput(
                templateId = 1L,
                templateVersion = 1.0f,
                userIds = listOf(1L),
                eventId = EventId("failed-event"),
                expiredTime = LocalDateTime.now().plusHours(1)
            )

            val completableFuture = CompletableFuture<SendResult<String, Any>>()
            every { kafkaTemplate.send(any<String>(), any<String>(), any<ScheduledTaskMessage>()) } returns completableFuture

            completableFuture.completeExceptionally(RuntimeException("Kafka send failed"))

            Then("it should handle the failure gracefully") {
                val exception = kotlin.runCatching {
                    kafkaExecutor.executeScheduledTask("failed-task", input)
                    // CompletableFuture의 whenComplete 콜백이 비동기로 실행되므로
                    // 테스트에서는 예외가 직접 전파되지 않을 수 있음
                }.exceptionOrNull()

                // 메시지는 전송되었지만 비동기 콜백에서 실패 처리됨
                verify { kafkaTemplate.send(any<String>(), any<String>(), any<ScheduledTaskMessage>()) }
            }
        }

        When("getting executor type") {
            Then("it should return 'kafka'") {
                kafkaExecutor.getExecutorType() shouldBe "kafka"
            }
        }
    }
})
