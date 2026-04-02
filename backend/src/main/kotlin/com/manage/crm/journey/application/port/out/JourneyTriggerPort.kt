package com.manage.crm.journey.application.port.out

/**
 * Journey 트리거 발행을 위한 공개 포트 (외부 모듈용)
 *
 * 타 모듈(event, user, segment)에서 Journey를 트리거할 때 사용합니다.
 * queue 내부 구현(JourneyTriggerQueuePublisher)을 추상화하여 결합도를 낮춥니다.
 *
 * ## 마이그레이션 전략 (T18에서 적용 예정)
 *
 * 1. 타 모듈이 이 인터페이스를 import하도록 변경
 * 2. queue 패키지의 JourneyTriggerQueuePublisher가 이 인터페이스를 구현
 * 3. 기존 JourneyEventPayload → JourneyTriggerEventPayload 매핑 추가
 *
 * @see com.manage.crm.journey.queue.JourneyTriggerQueuePublisher
 */
interface JourneyTriggerPort {
    suspend fun triggerByEvent(event: JourneyTriggerEventPayload)

    suspend fun triggerBySegmentContextChange(changedUserIds: List<Long> = emptyList())
}
