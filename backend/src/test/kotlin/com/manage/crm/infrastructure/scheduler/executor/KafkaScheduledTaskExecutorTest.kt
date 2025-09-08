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

            Then("it should attempt to send message even if Kafka is unavailable") {
                // Kafka 전송이 실패하는 경우를 시뮬레이션
                every { kafkaTemplate.send(any<String>(), any<String>(), any<ScheduledTaskMessage>()) } throws RuntimeException("Kafka broker unavailable")

                var thrownException: Exception? = null
                try {
                    kafkaExecutor.executeScheduledTask("failed-task", input)
                } catch (e: Exception) {
                    thrownException = e
                }

                // 실제로 Kafka 전송 실패시 예외가 발생하는지 확인 (실제 구현에 맞게)
                thrownException shouldNotBe null
                val exception = thrownException as RuntimeException
                exception::class shouldBe RuntimeException::class
                exception.message shouldBe "Error executing scheduled task: failed-task"
                exception.cause!!.message shouldBe "Kafka broker unavailable"
                verify { kafkaTemplate.send(any<String>(), any<String>(), any<ScheduledTaskMessage>()) }
            }
        }

        When("validating message content") {
            val input = NotificationEmailSendTimeOutEventInput(
                templateId = 999L,
                templateVersion = 2.5f,
                userIds = listOf(100L, 200L),
                eventId = EventId("validation-test"),
                expiredTime = LocalDateTime.of(2025, 6, 15, 14, 30, 0)
            )

            val messageSlot = slot<ScheduledTaskMessage>()
            val mockSendResult = mockk<SendResult<String, Any>>()
            val mockRecordMetadata = mockk<org.apache.kafka.clients.producer.RecordMetadata>()
            val completableFuture = CompletableFuture<SendResult<String, Any>>()

            every { mockSendResult.recordMetadata } returns mockRecordMetadata
            every { mockRecordMetadata.offset() } returns 456L
            every { kafkaTemplate.send(any<String>(), any<String>(), capture(messageSlot)) } returns completableFuture

            completableFuture.complete(mockSendResult)

            Then("it should create message with correct data") {
                kafkaExecutor.executeScheduledTask("validation-task", input)

                val capturedMessage = messageSlot.captured
                
                // 메시지 내용 상세 검증
                capturedMessage.taskId shouldBe "validation-task"
                capturedMessage.scheduleInfo shouldBe input
                capturedMessage.executedAt shouldNotBe 0L
                
                // scheduleInfo의 모든 필드 검증 (실제 데이터 무결성 확인)
                val scheduleInfo = capturedMessage.scheduleInfo as NotificationEmailSendTimeOutEventInput
                scheduleInfo.templateId shouldBe 999L
                scheduleInfo.templateVersion shouldBe 2.5f
                scheduleInfo.userIds shouldBe listOf(100L, 200L)
                scheduleInfo.eventId.value shouldBe "validation-test"
                scheduleInfo.expiredTime shouldBe LocalDateTime.of(2025, 6, 15, 14, 30, 0)
            }
        }

        When("getting executor type") {
            Then("it should return 'kafka'") {
                kafkaExecutor.getExecutorType() shouldBe "kafka"
            }
        }
    }
})
