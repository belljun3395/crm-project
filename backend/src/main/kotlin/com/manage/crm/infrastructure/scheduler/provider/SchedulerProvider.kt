package com.manage.crm.infrastructure.scheduler.provider

import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import com.manage.crm.infrastructure.scheduler.ScheduleName
import java.time.LocalDateTime

/**
 * 벤더 독립적인 스케줄러 제공자 인터페이스
 * 다양한 스케줄링 백엔드 (AWS EventBridge, Redis+Kafka, Quartz 등)를 지원할 수 있도록 추상화
 */
interface SchedulerProvider {

    /**
     * 새로운 스케줄을 생성합니다.
     * * @param name 스케줄 이름 (고유 식별자)
     * @param schedule 실행 시점
     * @param input 실행할 작업 정보
     * @return 생성된 스케줄의 식별자
     */
    fun createSchedule(name: String, schedule: LocalDateTime, input: ScheduleInfo): String

    /**
     * 등록된 모든 스케줄 목록을 조회합니다.
     * * @return 스케줄 이름 목록
     */
    fun browseSchedule(): List<ScheduleName>

    /**
     * 특정 스케줄을 삭제합니다.
     * * @param scheduleName 삭제할 스케줄 이름
     */
    fun deleteSchedule(scheduleName: ScheduleName)

    /**
     * 스케줄러 제공자의 타입을 반환합니다.
     * * @return 스케줄러 타입 (예: "aws", "redis-kafka", "quartz")
     */
    fun getProviderType(): String
}
