package com.manage.crm.infrastructure.scheduler.consumer

import com.manage.crm.email.application.dto.NotificationEmailSendTimeOutEventInput
import com.manage.crm.infrastructure.message.config.KafkaConfig
import com.manage.crm.infrastructure.scheduler.executor.ScheduledTaskMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Kafka에서 스케줄된 작업 메시지를 수신하여 실제 비즈니스 로직을 처리하는 Consumer
 * Redis+Kafka 스케줄러가 활성화된 경우에만 Bean이 생성됩니다.
 */
@Component
@ConditionalOnProperty(name = ["scheduler.provider"], havingValue = "redis-kafka")
class ScheduledTaskConsumer {

    private val log = KotlinLogging.logger {}

    @KafkaListener(
        topics = [KafkaConfig.SCHEDULED_TASKS_TOPIC],
        groupId = "crm-scheduler-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleScheduledTask(
        @Payload message: ScheduledTaskMessage,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        try {
            log.info {
                "Received scheduled task from Kafka - TaskId: ${message.taskId}, " +
                    "Topic: $topic, Partition: $partition, Offset: $offset"
            }

            // 스케줄 정보의 타입에 따라 적절한 처리 수행
            when (val scheduleInfo = message.scheduleInfo) {
                is NotificationEmailSendTimeOutEventInput -> {
                    handleNotificationEmailTimeout(scheduleInfo, message.taskId)
                }
                else -> {
                    log.warn { "Unknown schedule info type: ${scheduleInfo::class.simpleName} for task ${message.taskId}" }
                }
            }

            // 메시지 처리 성공 시 수동 커밋
            acknowledgment.acknowledge()
            log.info { "Successfully processed and acknowledged scheduled task: ${message.taskId}" }
        } catch (ex: Exception) {
            log.error(ex) { "Error processing scheduled task: ${message.taskId}" }
            // 에러 발생 시 `acknowledge`하지 않음으로써 메시지가 재처리되도록 함
            // TODO: implement DLQ handling
        }
    }

    /**
     * 이메일 알림 타임아웃 이벤트를 처리합니다.
     * 기존 SQS 기반 처리 로직과 동일한 동작을 수행해야 합니다.
     */
    private fun handleNotificationEmailTimeout(
        input: NotificationEmailSendTimeOutEventInput,
        taskId: String
    ) {
        log.info {
            "Processing notification email timeout - TaskId: $taskId, " +
                "TemplateId: ${input.templateId}, UserCount: ${input.userIds.size}"
        }

        // TODO: 실제 이메일 알림 타임아웃 처리 로직 구현
        // 기존 SQS 리스너에서 수행하던 동일한 로직을 여기서 수행해야 합니다.
        // 예: 이메일 발송, 상태 업데이트, 이벤트 발행 등

        // 임시로 로그만 출력 (실제 구현 시 제거)
        log.info { "Email notification timeout processed for template ${input.templateId}" }
    }
}
