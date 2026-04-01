package com.manage.crm.journey.application.port.out

import java.time.LocalDateTime

/**
 * Journey 트리거를 위한 이벤트 페이로드 (공개 계약)
 *
 * 타 모듈(event, user, segment)에서 Journey를 트리거할 때 사용하는 DTO입니다.
 * queue 내부 구현 타입(JourneyEventPayload)의 공개 인터페이스 역할을 합니다.
 */
data class JourneyTriggerEventPayload(
    val id: Long,
    val name: String,
    val userId: Long,
    val properties: List<JourneyTriggerEventPropertyPayload>,
    val createdAt: LocalDateTime?,
)

data class JourneyTriggerEventPropertyPayload(
    val key: String,
    val value: String,
)
