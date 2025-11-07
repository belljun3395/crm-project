# Campaign Dashboard - 구현 상세

## 📁 파일 구조

```
backend/src/main/kotlin/com/manage/crm/event/
│
├── application/                              # UseCase Layer
│   ├── GetCampaignDashboardUseCase.kt       ✅ 대시보드 조회
│   ├── GetCampaignSummaryUseCase.kt         ✅ 요약 정보 조회
│   ├── GetStreamStatusUseCase.kt            ✅ 스트림 상태 조회
│   └── dto/
│       ├── GetCampaignDashboardUseCaseDto.kt
│       ├── GetCampaignSummaryUseCaseDto.kt
│       └── GetStreamStatusUseCaseDto.kt
│
├── controller/                               # Presentation Layer
│   ├── CampaignDashboardController.kt       ✅ REST API + SSE
│   └── dto/
│       ├── CampaignEventData.kt
│       ├── CampaignSummaryResponse.kt
│       └── StreamStatusResponse.kt
│
├── domain/                                   # Domain Layer
│   ├── CampaignDashboardMetrics.kt          ✅ 메트릭 엔티티
│   ├── MetricType.kt                        ✅ Enum (EVENT_COUNT)
│   ├── TimeWindowUnit.kt                    ✅ Enum (시간 단위)
│   └── repository/
│       └── CampaignDashboardMetricsRepository.kt
│
└── service/                                  # Service Layer
    ├── CampaignDashboardService.kt          ✅ 비즈니스 로직
    ├── CampaignDashboardStreamService.kt    ✅ Redis Stream 관리
    └── dto/
        └── CampaignDashboardSummary.kt

backend/src/test/kotlin/com/manage/crm/event/
└── service/
    └── CampaignDashboardServiceTest.kt      ✅ 단위 테스트
```

---

## ✅ 구현 체크리스트

### Layer 1: Domain
- [x] `CampaignDashboardMetrics` 엔티티
  - [x] JPA 매핑
  - [x] Index 설정
  - [x] `incrementValue()` 메서드
- [x] `MetricType` Enum
- [x] `TimeWindowUnit` Enum
- [x] Repository 인터페이스
  - [x] `findByCampaignIdAndTimeWindowStartBetween()`
  - [x] `findByCampaignIdAndTimeWindowUnitAndTimeWindowStartAfter()`
  - [x] `findAllByCampaignIdOrderByTimeWindowStartDesc()`

### Layer 2: Service
- [x] `CampaignDashboardStreamService`
  - [x] `publishEvent()` - Redis Stream 발행
  - [x] `streamEvents()` - 실시간 스트리밍
  - [x] `getStreamLength()` - Stream 길이 조회
  - [x] `trimStream()` - 자동 메모리 관리
  - [x] `mapRecordToEvent()` - Record → Event 변환
- [x] `CampaignDashboardService`
  - [x] `publishCampaignEvent()` - 통합 발행 로직
  - [x] `updateMetricsForEvent()` - DB 메트릭 업데이트
  - [x] `getMetricsForCampaign()` - 시간 범위 조회
  - [x] `getMetricsByTimeUnit()` - 시간 단위 조회
  - [x] `getCampaignSummary()` - 요약 정보
  - [x] `streamCampaignEvents()` - SSE용 Flux 반환
  - [x] 자동 trim (100개 이벤트마다)

### Layer 3: UseCase
- [x] `GetCampaignDashboardUseCase`
  - [x] Input/Output DTO
  - [x] 시간 범위/단위 처리 로직
- [x] `GetCampaignSummaryUseCase`
  - [x] Input/Output DTO
- [x] `GetStreamStatusUseCase`
  - [x] Input/Output DTO

### Layer 4: Controller
- [x] `CampaignDashboardController`
  - [x] UseCase 패턴 적용
  - [x] Swagger 문서화
  - [x] 4개 엔드포인트 구현
    - [x] GET `/dashboard` - 대시보드 조회
    - [x] GET `/dashboard/stream` - SSE 스트리밍
    - [x] GET `/dashboard/summary` - 요약 정보
    - [x] GET `/dashboard/stream/status` - 상태 조회

### Integration
- [x] `PostEventUseCase` 통합
  - [x] 이벤트 발행 시 `publishCampaignEvent()` 호출
  - [x] 에러 처리 (실패 시에도 메인 플로우 유지)

### Testing
- [x] `CampaignDashboardServiceTest`
  - [x] `publishCampaignEvent()` 테스트
  - [x] `getCampaignSummary()` 테스트
  - [x] Mock 설정 (streamService, repository)

### Documentation
- [x] README.md - 전체 개요
- [x] API.md - API 명세서
- [x] QUICK_START.md - 빠른 시작 가이드
- [x] IMPLEMENTATION.md - 구현 상세 (현재 문서)

---

## 🔧 주요 구현 패턴

### 1. UseCase 패턴
```kotlin
@Component
class GetCampaignSummaryUseCase(
    private val campaignDashboardService: CampaignDashboardService
) {
    suspend fun execute(input: GetCampaignSummaryUseCaseIn): GetCampaignSummaryUseCaseOut {
        val summary = campaignDashboardService.getCampaignSummary(input.campaignId)
        return GetCampaignSummaryUseCaseOut(
            campaignId = summary.campaignId,
            totalEvents = summary.totalEvents,
            eventsLast24Hours = summary.eventsLast24Hours,
            eventsLast7Days = summary.eventsLast7Days,
            lastUpdated = summary.lastUpdated
        )
    }
}
```

**특징:**
- Controller와 Service 사이의 명확한 경계
- Input/Output DTO 사용
- 단일 책임 원칙 (SRP)

### 2. Redis Stream 패턴
```kotlin
// 발행
val record: StringRecord = StreamRecords.string(
    mapOf("campaignId" to "1", "eventId" to "100", ...)
).withStreamKey("campaign:dashboard:stream:1")

reactiveRedisTemplate.opsForStream<String, Any>()
    .add(record)
    .awaitSingle()

// 구독
reactiveRedisTemplate.opsForStream<String, Any>()
    .read(StreamOffset.fromStart(streamKey))
    .map { record -> mapRecordToEvent(record) }
```

**특징:**
- Reactive Stream (Non-blocking)
- MapRecord 타입 사용
- 자동 직렬화/역직렬화

### 3. SSE (Server-Sent Events) 패턴
```kotlin
fun streamCampaignDashboard(...): Flux<ServerSentEvent<CampaignEventData>> {
    return campaignDashboardService.streamCampaignEvents(campaignId)
        .map { event ->
            ServerSentEvent.builder<CampaignEventData>()
                .id(event.eventId.toString())
                .event("campaign-event")
                .data(CampaignEventData(...))
                .build()
        }
        .timeout(duration)
        .onErrorResume { ... }
}
```

**특징:**
- WebFlux Reactive
- Event ID 포함 (재연결 지원)
- 타임아웃 처리
- 에러 이벤트 전송

### 4. 메트릭 집계 패턴
```kotlin
private suspend fun updateMetricsForEvent(event: CampaignDashboardEvent) {
    val timeWindows = listOf(
        TimeWindowUnit.HOUR to calculateTimeWindow(event.timestamp, TimeWindowUnit.HOUR),
        TimeWindowUnit.DAY to calculateTimeWindow(event.timestamp, TimeWindowUnit.DAY)
    )

    timeWindows.forEach { (unit, window) ->
        updateOrCreateMetric(
            campaignId = event.campaignId,
            metricType = MetricType.EVENT_COUNT,
            timeWindowUnit = unit,
            timeWindowStart = window.first,
            timeWindowEnd = window.second,
            incrementBy = 1
        )
    }
}
```

**특징:**
- 이벤트 발생 시 즉시 집계
- 여러 시간 단위 동시 업데이트
- Upsert 패턴 (없으면 생성, 있으면 증가)

### 5. 자동 메모리 관리 패턴
```kotlin
suspend fun publishCampaignEvent(event: CampaignDashboardEvent) {
    streamService.publishEvent(event)
    updateMetricsForEvent(event)

    // 100개 이벤트마다 trim
    val streamLength = streamService.getStreamLength(event.campaignId)
    if (streamLength % 100 == 0L && streamLength > 0) {
        streamService.trimStream(event.campaignId, maxLength = 10000)
    }
}
```

**특징:**
- 조건부 실행 (100의 배수)
- 최대 10,000개 유지
- 비동기 처리 (성능 영향 최소화)

---

## 🔍 핵심 로직 흐름

### Flow 1: 이벤트 발행 → 실시간 스트리밍
```
[Client] POST /api/v1/events
    ↓
[PostEventUseCase]
    ├─ Event 저장
    └─ publishCampaignEvent(event)
        ↓
[CampaignDashboardService]
    ├─ Redis Stream 발행 ────────┐
    ├─ DB 메트릭 업데이트         │
    └─ Auto Trim (100개마다)      │
                                  │
[SSE Client] ←─────────────────┘
    ↓
실시간으로 이벤트 수신
```

### Flow 2: 대시보드 조회
```
[Client] GET /api/v1/campaigns/1/dashboard?timeWindowUnit=HOUR
    ↓
[CampaignDashboardController]
    ↓
[GetCampaignDashboardUseCase]
    ├─ getMetricsByTimeUnit()
    └─ getCampaignSummary()
        ↓
[CampaignDashboardService]
    ├─ Repository 조회
    └─ 데이터 집계
        ↓
[Response] JSON
```

### Flow 3: 실시간 모니터링
```
[Client] GET /api/v1/campaigns/1/dashboard/stream (SSE)
    ↓
[CampaignDashboardController]
    ↓
[CampaignDashboardService]
    ↓
[CampaignDashboardStreamService]
    ├─ Redis Stream 구독
    └─ Flux<Event> 반환
        ↓
[Controller] → SSE 변환
        ↓
[Client] EventSource로 수신
```

---

## 🎯 설계 결정 사항

### 1. Redis Stream vs Kafka
**선택**: Redis Stream

**이유**:
- ✅ 간단한 설정 (별도 클러스터 불필요)
- ✅ 낮은 레이턴시
- ✅ 메모리 기반 (빠른 읽기/쓰기)
- ❌ Kafka 대비 낮은 처리량 (acceptable for current scale)

### 2. DB 집계 vs Stream만 사용
**선택**: 하이브리드 (Stream + DB)

**이유**:
- Stream: 실시간 모니터링
- DB: 과거 데이터 분석, 집계 쿼리
- Trade-off: 약간의 중복 저장, but 쿼리 성능 향상

### 3. SSE vs WebSocket
**선택**: SSE (Server-Sent Events)

**이유**:
- ✅ 단방향 통신으로 충분 (서버 → 클라이언트)
- ✅ HTTP 프로토콜 사용 (방화벽 친화적)
- ✅ 자동 재연결 지원
- ✅ EventSource API (표준)
- ❌ WebSocket 대비 양방향 통신 불가 (not needed)

### 4. Sync vs Async 이벤트 발행
**선택**: Async (비동기)

**이유**:
- ✅ 메인 플로우 블로킹 방지
- ✅ Stream 실패 시에도 이벤트 저장 성공
- ❌ 약간의 지연 (acceptable)

### 5. UseCase 패턴 적용 여부
**선택**: 적용

**이유**:
- ✅ 프로젝트 전체 컨벤션 일치
- ✅ Controller-Service 분리
- ✅ 테스트 용이성
- ❌ 약간의 보일러플레이트 (acceptable)

### 6. Consumer Group 사용 여부
**선택**: 미사용 (제거)

**이유**:
- ✅ 현재 목적: 실시간 모니터링 (SSE Broadcast)
- ✅ 모든 클라이언트가 같은 이벤트를 봐야 함
- ✅ 코드 단순화
- ❌ 작업 분산 불가 (향후 필요 시 추가)

**상세**: [CONSUMER_GROUP.md](CONSUMER_GROUP.md) 참고

**현재 동작**:
```
Redis Stream → 모든 클라이언트가 동일한 이벤트 수신 (Broadcast)
```

**향후 추가 시**:
```
Redis Stream → Consumer Group → Worker들이 이벤트 분산 처리
```

---

## 📊 성능 고려사항

### Redis Stream
- **최대 이벤트 수**: 10,000개/campaign
- **Trim 주기**: 100개마다
- **메모리 사용량**: ~100MB/campaign (예상)

### Database
- **Index**: `(campaign_id, time_window_start, time_window_end)`
- **파티셔닝**: 미구현 (향후 시간 기반 파티셔닝 고려)

### SSE
- **동시 연결**: 제한 없음 (향후 Rate Limiting 필요)
- **타임아웃**: 기본 1시간
- **재연결**: 클라이언트 측 처리

---

## 🚨 알려진 제약사항

1. **메트릭 타입 제한**
   - 현재 `EVENT_COUNT`만 지원
   - 향후 확장: CLICK_RATE, CONVERSION_RATE 등

2. **Consumer Group 미사용**
   - 병렬 처리 불가
   - 향후 추가 예정

3. **과거 데이터 재집계 미지원**
   - 배치 작업 필요
   - 수동 실행만 가능

4. **인증/인가 미구현**
   - 모든 엔드포인트 public
   - 향후 JWT 적용 예정

---

## 🔄 향후 개선 계획

### Short-term (1-2주)
- [ ] Consumer Group 추가 (병렬 처리)
- [ ] Rate Limiting
- [ ] 메트릭 타입 확장

### Mid-term (1-2개월)
- [ ] 과거 데이터 재집계 배치
- [ ] Redis Cluster 설정
- [ ] Grafana 연동

### Long-term (3개월+)
- [ ] ML 기반 이상 탐지
- [ ] 예측 분석
- [ ] A/B 테스트 통합
