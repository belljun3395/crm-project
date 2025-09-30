package com.manage.crm.infrastructure.scheduler.executor

import com.manage.crm.infrastructure.message.config.KafkaConfig
import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * Kafka를 이용한 스케줄된 작업 실행자
 * Redis+Kafka 스케줄러가 활성화된 경우에만 Bean이 생성됩니다.
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class KafkaScheduledTaskExecutor(
    @Qualifier("scheduledTaskKafkaTemplate")
    private val kafkaTemplate: KafkaTemplate<String, ScheduledTaskMessage>
) : ScheduledTaskExecutor {

    private val log = KotlinLogging.logger {}

    override fun executeScheduledTask(taskId: String, scheduleInfo: ScheduleInfo) {
        try {
            val message = ScheduledTaskMessage(
                taskId = taskId,
                scheduleInfo = scheduleInfo,
                executedAt = System.currentTimeMillis()
            )

            kafkaTemplate.send(KafkaConfig.SCHEDULED_TASKS_TOPIC, taskId, message)
                .whenComplete { result, ex ->
                    if (ex == null) {
                        log.info { "Successfully sent scheduled task $taskId to Kafka. Offset: ${result.recordMetadata.offset()}" }
                    } else {
                        log.error(ex) { "Failed to send scheduled task $taskId to Kafka" }
                        // 비동기 실패는 예외 전파 대신 로깅으로 처리
                        // TODO: 메트릭 추가 - meterRegistry.counter("scheduler.kafka.send.fail").increment()
                    }
                }
        } catch (ex: Exception) {
            log.error(ex) { "Error executing scheduled task $taskId" }
            throw RuntimeException("Error executing scheduled task: $taskId", ex)
        }
    }

    override fun getExecutorType(): String = "kafka"
}

/**
 * Kafka로 전송되는 스케줄된 작업 메시지
 */
data class ScheduledTaskMessage(
    val taskId: String,
    val scheduleInfo: ScheduleInfo,
    val executedAt: Long
)
