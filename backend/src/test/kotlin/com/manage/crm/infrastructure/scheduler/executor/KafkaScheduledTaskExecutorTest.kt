package com.manage.crm.infrastructure.scheduler.executor

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class KafkaScheduledTaskExecutorTest : FeatureSpec({
    lateinit var kafkaTemplate: KafkaTemplate<String, ScheduledTaskEvent>
    lateinit var executor: KafkaScheduledTaskExecutor

    beforeTest {
        kafkaTemplate = mockk(relaxed = true)
        executor = KafkaScheduledTaskExecutor(kafkaTemplate)
    }

    feature("KafkaScheduledTaskExecutor#execute") {
        scenario("should publish event to Kafka and return success") {
            // given
            val eventId = EventId("test-event-123")
            val scheduleTime = LocalDateTime.now()
            val input = NotificationEmailSendTimeOutEventInput(
                templateId = 1L,
                templateVersion = 1.0f,
                userIds = listOf(1L, 2L),
                eventId = eventId,
                expiredTime = scheduleTime
            )

            val event = ScheduledTaskEvent(
                scheduleName = eventId.value,
                scheduleTime = scheduleTime,
                payload = input
            )

            val recordMetadata = RecordMetadata(
                TopicPartition(KafkaScheduledTaskExecutor.TOPIC, 0),
                0L, 0, 0L, 0, 0
            )
            val sendResult = SendResult<String, ScheduledTaskEvent>(null, recordMetadata)
            val future = CompletableFuture.completedFuture(sendResult)

            every { kafkaTemplate.send(any<String>(), any(), any()) } returns future

            // when
            val result = executor.execute(event)

            // then
            result.get() shouldBe true

            verify(exactly = 1) {
                kafkaTemplate.send(KafkaScheduledTaskExecutor.TOPIC, eventId.value, event)
            }
        }

        scenario("should return false when Kafka send fails") {
            // given
            val eventId = EventId("test-event-fail")
            val scheduleTime = LocalDateTime.now()
            val input = NotificationEmailSendTimeOutEventInput(
                templateId = 1L,
                templateVersion = 1.0f,
                userIds = listOf(1L),
                eventId = eventId,
                expiredTime = scheduleTime
            )

            val event = ScheduledTaskEvent(
                scheduleName = eventId.value,
                scheduleTime = scheduleTime,
                payload = input
            )

            val future = CompletableFuture<SendResult<String, ScheduledTaskEvent>>()
            future.completeExceptionally(RuntimeException("Kafka broker unavailable"))

            every { kafkaTemplate.send(any<String>(), any(), any()) } returns future

            // when
            val result = executor.execute(event)

            // then
            result.get() shouldBe false
        }
    }

    feature("KafkaScheduledTaskExecutor#executeBatch") {
        scenario("should execute multiple events and return list of futures") {
            // given
            val events = (1..3).map { i ->
                val eventId = EventId("batch-event-$i")
                ScheduledTaskEvent(
                    scheduleName = eventId.value,
                    scheduleTime = LocalDateTime.now(),
                    payload = NotificationEmailSendTimeOutEventInput(
                        templateId = i.toLong(),
                        templateVersion = 1.0f,
                        userIds = listOf(i.toLong()),
                        eventId = eventId,
                        expiredTime = LocalDateTime.now()
                    )
                )
            }

            val recordMetadata = RecordMetadata(
                TopicPartition(KafkaScheduledTaskExecutor.TOPIC, 0),
                0L, 0, 0L, 0, 0
            )
            val sendResult = SendResult<String, ScheduledTaskEvent>(null, recordMetadata)
            val future = CompletableFuture.completedFuture(sendResult)

            every { kafkaTemplate.send(any<String>(), any(), any()) } returns future

            // when
            val results = executor.executeBatch(events)

            // then
            results.size shouldBe 3
            results.forEach { it.get() shouldBe true }

            verify(exactly = 3) {
                kafkaTemplate.send(any<String>(), any(), any())
            }
        }
    }
})
