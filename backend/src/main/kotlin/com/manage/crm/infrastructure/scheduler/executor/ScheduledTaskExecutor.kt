package com.manage.crm.infrastructure.scheduler.executor

import com.manage.crm.infrastructure.scheduler.ScheduleInfo

/**
 * 스케줄된 작업을 실제로 실행하는 인터페이스
 * Redis+Kafka, SQS 등 다양한 메시징 시스템을 지원할 수 있도록 추상화
 */
interface ScheduledTaskExecutor {

    /**
     * 스케줄된 작업을 실행합니다.
     * 일반적으로 메시지 큐나 이벤트 시스템을 통해 실행됩니다.
     * * @param taskId 작업 식별자
     * @param scheduleInfo 실행할 작업 정보
     */
    fun executeScheduledTask(taskId: String, scheduleInfo: ScheduleInfo)

    /**
     * 실행자의 타입을 반환합니다.
     * * @return 실행자 타입 (예: "kafka", "sqs", "direct")
     */
    fun getExecutorType(): String
}
