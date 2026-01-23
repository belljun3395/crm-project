package com.manage.crm.infrastructure.scheduler.executor

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.email.domain.vo.EventId
import com.manage.crm.infrastructure.scheduler.event.ScheduledTaskEvent
import com.manage.crm.infrastructure.scheduler.provider.RedisSchedulerProvider
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.test.runTest
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class KafkaScheduledTaskExecutorTest {

    private lateinit var kafkaTemplate: KafkaTemplate<String, ScheduledTaskEvent>
    private lateinit var executor: KafkaScheduledTaskExecutor

    private lateinit var scheduleData: RedisSchedulerProvider.ScheduleData

    @BeforeEach
    fun setup() {
        kafkaTemplate = mock()
        executor = KafkaScheduledTaskExecutor(kafkaTemplate)
        scheduleData = RedisSchedulerProvider.ScheduleData(
            name = "schedule-1",
            scheduleTime = LocalDateTime.now(),
            payload = NotificationEmailSendTimeOutEventInput(
                templateId = 1L,
                templateVersion = 1.0f,
                userIds = listOf(1L, 2L),
                eventId = EventId("schedule-1"),
                expiredTime = LocalDateTime.now()
            )
        )
    }

    @Test
    fun `execute should return true when kafka publish succeeds`() = runTest {
        val sendResult: SendResult<String, ScheduledTaskEvent> = mock()
        val metadata = RecordMetadata(TopicPartition("scheduled-tasks", 0), 0, 0, 0, 0, 0, 0)

        whenever(sendResult.recordMetadata).thenReturn(metadata)
        whenever(kafkaTemplate.send(eq("scheduled-tasks"), eq(scheduleData.name), any()))
            .thenReturn(CompletableFuture.completedFuture(sendResult))

        executor.execute(scheduleData).shouldBeTrue()
        verify(kafkaTemplate).send(eq("scheduled-tasks"), eq(scheduleData.name), any())
    }

    @Test
    fun `execute should return false when kafka publish throws`() = runTest {
        whenever(kafkaTemplate.send(any<String>(), any(), any())).thenThrow(RuntimeException("kafka down"))

        executor.execute(scheduleData).shouldBeFalse()
    }
}
