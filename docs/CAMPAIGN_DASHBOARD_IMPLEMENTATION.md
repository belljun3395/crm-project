# 실시간 캠페인 대시보드 구현 문서

## 📋 목차
1. [개요](#개요)
2. [아키텍처](#아키텍처)
3. [기술 스택](#기술-스택)
4. [데이터베이스 설계](#데이터베이스-설계)
5. [주요 컴포넌트](#주요-컴포넌트)
6. [API 명세](#api-명세)
7. [기술적 의사결정](#기술적-의사결정)
8. [성능 고려사항](#성능-고려사항)
9. [보안 및 안정성](#보안-및-안정성)
10. [테스트 전략](#테스트-전략)
11. [운영 가이드](#운영-가이드)
12. [개선 방향](#개선-방향)

---

## 개요

### 목적
캠페인별 이벤트 발생 현황을 실시간으로 모니터링하고, 시간 단위별 집계된 메트릭 정보를 제공하는 대시보드 시스템을 구현합니다.

### 주요 기능
- ✅ Redis Stream을 활용한 실시간 이벤트 발행/구독
- ✅ 시간 단위별(분/시간/일/주/월) 메트릭 자동 집계
- ✅ RESTful API를 통한 대시보드 데이터 조회
- ✅ Server-Sent Events(SSE)를 통한 실시간 스트리밍
- ✅ 데이터베이스 영구 저장 및 히스토리 관리

### 해결하는 문제
1. **실시간성**: 캠페인 이벤트가 발생하는 즉시 대시보드에 반영
2. **확장성**: Redis Stream을 통한 비동기 처리로 이벤트 생성 성능 보장
3. **분석 용이성**: 시간 단위별 집계로 트렌드 분석 가능
4. **데이터 일관성**: 이벤트 발생 시점과 메트릭 업데이트의 원자성 보장

---

## 아키텍처

### 시스템 구조도

```
┌─────────────────┐
│   Event 발생    │
│ (PostEventUseCase)│
└────────┬────────┘
         │
         ├─────────────────────┬──────────────────────┐
         │                     │                      │
         ▼                     ▼                      ▼
┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ CampaignEvents  │  │  Redis Stream    │  │ Dashboard Metrics│
│  DB 저장        │  │  이벤트 발행     │  │   자동 집계      │
└─────────────────┘  └────────┬─────────┘  └──────────────────┘
                              │
                              ▼
                    ┌──────────────────────┐
                    │  Stream Consumers    │
                    │  (실시간 처리)        │
                    └──────────────────────┘
                              │
                              ▼
                    ┌──────────────────────┐
                    │   SSE Endpoint       │
                    │  (클라이언트 스트림)  │
                    └──────────────────────┘
```

### 데이터 플로우

#### 1. 이벤트 발생 시 (Write Path)
```
Campaign Event 발생
    ↓
PostEventUseCase.setCampaignEvent()
    ↓
    ├─→ CampaignEvents DB 저장 (동기)
    │
    └─→ CampaignDashboardService.publishCampaignEvent()
            ↓
            ├─→ Redis Stream 발행 (비동기)
            │       └─→ Stream Key: campaign:dashboard:stream:{campaignId}
            │
            └─→ 메트릭 자동 업데이트
                    ↓
                    CampaignDashboardMetrics 테이블 UPSERT
                    (시간 윈도우별: HOUR, DAY)
```

#### 2. 대시보드 조회 시 (Read Path)
```
클라이언트 요청
    ↓
GET /api/v1/campaigns/{id}/dashboard
    ↓
GetCampaignDashboardUseCase
    ↓
CampaignDashboardMetricsRepository 쿼리
    ↓
    ├─→ 시간 범위 필터링
    ├─→ 시간 단위 필터링
    └─→ 메트릭 타입별 집계
    ↓
응답 반환 (JSON)
```

#### 3. 실시간 스트리밍 시 (Streaming Path)
```
클라이언트 SSE 연결
    ↓
GET /api/v1/campaigns/{id}/dashboard/stream
Header: Last-Event-ID: {id}
    ↓
CampaignDashboardStreamService.streamEvents(lastEventId)
    ↓
ReactiveRedisTemplate.opsForStream().read()
    ↓
Flux<CampaignDashboardEvent> 스트림 생성
    ↓
SSE 이벤트로 변환 및 전송
    ↓
클라이언트에 실시간 전달
```

---

## 기술 스택

### 백엔드 프레임워크
- **Spring Boot 3.x** with WebFlux (Reactive Stack)
- **Kotlin 1.9+** with Coroutines
- **R2DBC** (Reactive Relational Database Connectivity)
- **Spring Data Redis Reactive**

### 데이터 저장소
- **MySQL 8.0+**: 메트릭 영구 저장
- **Redis Cluster 7.0+**:
  - Redis Streams (이벤트 스트리밍)
  - Redis Hash (캠페인 캐싱)

### 주요 라이브러리
- **Lettuce**: Redis 클라이언트 (클러스터 모드)
- **Jackson**: JSON 직렬화/역직렬화
- **Kotlin Coroutines**: 비동기 처리
- **Reactor**: Reactive Streams 구현

---

## 데이터베이스 설계

### 테이블: `campaign_dashboard_metrics`

```sql
CREATE TABLE campaign_dashboard_metrics
(
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id            BIGINT       NOT NULL,
    metric_type            VARCHAR(50)  NOT NULL COMMENT 'EVENT_COUNT, USER_COUNT 등',
    metric_value           BIGINT       NOT NULL DEFAULT 0,
    time_window_start      DATETIME(6)  NOT NULL COMMENT '시간 윈도우 시작',
    time_window_end        DATETIME(6)  NOT NULL COMMENT '시간 윈도우 종료',
    time_window_unit       VARCHAR(20)  NOT NULL COMMENT 'MINUTE, HOUR, DAY 등',
    created_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                        ON UPDATE CURRENT_TIMESTAMP(6),

    -- 성능 최적화를 위한 인덱스
    INDEX idx_campaign_id_metric_type (campaign_id, metric_type),
    INDEX idx_time_window (time_window_start, time_window_end),

    -- 중복 방지를 위한 유니크 제약
    UNIQUE KEY unique_campaign_metric_time (
        campaign_id,
        metric_type,
        time_window_start,
        time_window_end
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 인덱스 전략

#### 1. `idx_campaign_id_metric_type`
```sql
INDEX (campaign_id, metric_type)
```
- **용도**: 특정 캠페인의 특정 메트릭 타입 조회 시 사용
- **쿼리 예**: 캠페인 1번의 모든 EVENT_COUNT 조회
- **카디널리티**: campaign_id (중간), metric_type (낮음)

#### 2. `idx_time_window`
```sql
INDEX (time_window_start, time_window_end)
```
- **용도**: 시간 범위 기반 조회 시 사용
- **쿼리 예**: 2024-01-01 ~ 2024-01-07 사이의 모든 메트릭 조회
- **카디널리티**: time_window_start (매우 높음)

#### 3. `unique_campaign_metric_time`
```sql
UNIQUE KEY (campaign_id, metric_type, time_window_start, time_window_end)
```
- **용도**: 동일한 시간 윈도우에 대한 중복 메트릭 생성 방지
- **부수 효과**: 복합 인덱스로 활용 가능 (커버링 인덱스)

### 시간 윈도우 설계

각 이벤트 발생 시, 다음 2개의 시간 윈도우에 대해 메트릭이 생성/업데이트됩니다:

```kotlin
// 예: 2024-01-15 14:35:23 에 이벤트 발생

// HOUR 윈도우
time_window_start: 2024-01-15 14:00:00
time_window_end:   2024-01-15 15:00:00
time_window_unit:  HOUR

// DAY 윈도우
time_window_start: 2024-01-15 00:00:00
time_window_end:   2024-01-16 00:00:00
time_window_unit:  DAY
```

### 데이터 크기 추정

**가정**:
- 캠페인 수: 1,000개
- 평균 이벤트 발생률: 100 events/campaign/day
- 메트릭 타입: 3종류 (EVENT_COUNT, UNIQUE_USER_COUNT, TOTAL_USER_COUNT)
- 시간 윈도우: 2종류 (HOUR, DAY)

**일일 생성되는 레코드 수**:
```
1,000 캠페인 × 2 시간윈도우 × 3 메트릭타입 × 1일
= 6,000 rows/day
```

**월간 데이터 크기** (30일 기준):
```
6,000 rows/day × 30일 = 180,000 rows/month
레코드 당 평균 크기: ~200 bytes
총 크기: 36 MB/month (인덱스 제외)
```

**1년 후 데이터 크기**: 약 432 MB (관리 가능한 수준)

---

## 주요 컴포넌트

### 1. Domain Layer

#### `CampaignDashboardMetrics.kt`
```kotlin
@Table("campaign_dashboard_metrics")
class CampaignDashboardMetrics(
    var campaignId: Long,
    var metricType: MetricType,      // EVENT_COUNT, USER_COUNT 등
    var metricValue: Long,            // 집계된 값
    var timeWindowStart: LocalDateTime,
    var timeWindowEnd: LocalDateTime,
    var timeWindowUnit: TimeWindowUnit
) {
    fun incrementValue(incrementBy: Long = 1) {
        this.metricValue += incrementBy
    }
}
```

**특징**:
- R2DBC 엔티티 (Reactive)
- `incrementValue()`: 메트릭 값 증가 로직 캡슐화
- 불변성 보장을 위한 `val` 사용 (id, timestamps 제외)

#### `MetricType` Enum
```kotlin
enum class MetricType {
    EVENT_COUNT,          // 이벤트 총 개수
    UNIQUE_USER_COUNT,    // 고유 사용자 수
    TOTAL_USER_COUNT      // 총 사용자 수 (중복 포함)
}
```

#### `TimeWindowUnit` Enum
```kotlin
enum class TimeWindowUnit {
    MINUTE,  // 1분 단위
    HOUR,    // 1시간 단위
    DAY,     // 1일 단위
    WEEK,    // 1주 단위
    MONTH    // 1월 단위
}
```

### 2. Infrastructure Layer

#### `CampaignDashboardStreamService.kt`

**핵심 메서드**:

##### `publishEvent()`
```kotlin
suspend fun publishEvent(event: CampaignDashboardEvent) {
    val streamKey = "campaign:dashboard:stream:${event.campaignId}"
    val record = StreamRecords.string(
        mapOf(
            "campaignId" to event.campaignId.toString(),
            "eventId" to event.eventId.toString(),
            "userId" to event.userId.toString(),
            "eventName" to event.eventName,
            "timestamp" to event.timestamp.format(ISO_LOCAL_DATE_TIME)
        )
    ).withStreamKey(streamKey)

    reactiveRedisTemplate.opsForStream<String, String>()
        .add(record)
        .awaitSingle()
}
```

**기술적 특징**:
- **비동기 발행**: `awaitSingle()`로 코루틴 컨텍스트에서 실행
- **캠페인별 스트림 분리**: 각 캠페인마다 독립적인 스트림 키 사용
- **타임스탬프 직렬화**: ISO 8601 형식으로 저장하여 파싱 용이

##### `streamEvents()`
```kotlin
fun streamEvents(campaignId: Long, duration: Duration): Flux<CampaignDashboardEvent> {
    val streamKey = getStreamKey(campaignId)

    return reactiveRedisTemplate.opsForStream<String, String>()
        .read(String::class.java, StreamOffset.fromStart(streamKey))
        .map { record -> mapRecordToEvent(record) }
        .timeout(duration)
        .onErrorResume { error ->
            log.error("Error streaming events", error)
            Flux.empty()
        }
}
```

**기술적 특징**:
- **Reactor Flux 반환**: Reactive Streams 표준 준수
- **타임아웃 설정**: 리소스 누수 방지
- **에러 핸들링**: `onErrorResume`으로 graceful degradation

##### `trimStream()`
```kotlin
suspend fun trimStream(campaignId: Long, maxLength: Long = 10000) {
    reactiveRedisTemplate.opsForStream<String, String>()
        .trim(streamKey, maxLength)
        .awaitFirstOrNull()
}
```

**메모리 관리**:
- 스트림이 무한정 커지는 것을 방지
- 기본값: 최근 10,000개 이벤트만 유지
- 주기적으로 호출하여 메모리 사용량 제어

### 3. Service Layer

#### `CampaignDashboardService.kt`

**핵심 로직**: 시간 윈도우 계산

```kotlin
private fun calculateTimeWindow(
    timestamp: LocalDateTime,
    unit: TimeWindowUnit
): Pair<LocalDateTime, LocalDateTime> {
    return when (unit) {
        TimeWindowUnit.HOUR -> {
            val start = timestamp.truncatedTo(ChronoUnit.HOURS)
            val end = start.plusHours(1)
            start to end
        }
        TimeWindowUnit.DAY -> {
            val start = timestamp.truncatedTo(ChronoUnit.DAYS)
            val end = start.plusDays(1)
            start to end
        }
        // ... 다른 단위들
    }
}
```

**기술적 고려사항**:
- `truncatedTo()`: 정확한 시간 윈도우 경계 계산
- **타임존 이슈**: 현재는 시스템 타임존 사용 (개선 필요)

**메트릭 업데이트 로직**:

```kotlin
private suspend fun updateOrCreateMetric(
    campaignId: Long,
    metricType: MetricType,
    timeWindowUnit: TimeWindowUnit,
    timeWindowStart: LocalDateTime,
    timeWindowEnd: LocalDateTime,
    incrementBy: Long = 1
) {
    // 기존 메트릭 조회
    val existing = repository.findByCampaignIdAndMetricTypeAndTimeWindowStartAndTimeWindowEnd(
        campaignId, metricType, timeWindowStart, timeWindowEnd
    )

    if (existing != null) {
        // 기존 메트릭 업데이트 (Race Condition 주의!)
        existing.incrementValue(incrementBy)
        repository.save(existing)
    } else {
        // 새 메트릭 생성
        val newMetric = CampaignDashboardMetrics.new(...)
        repository.save(newMetric)
    }
}
```

**동시성 문제**:
⚠️ **Race Condition 존재**: 동일 시간 윈도우에 동시 이벤트 발생 시 카운트 누락 가능
- 현재: 단순 SELECT → UPDATE 패턴
- 개선안: `ON DUPLICATE KEY UPDATE` 또는 낙관적 잠금 사용

### 4. Application Layer

#### `GetCampaignDashboardUseCase.kt`

**책임**:
- 비즈니스 로직 조율
- 입력 파라미터 검증 및 변환
- DTO 변환

**실행 플로우**:
```kotlin
suspend fun execute(input: GetCampaignDashboardUseCaseIn): GetCampaignDashboardUseCaseOut {
    // 1. 쿼리 조건에 따라 적절한 서비스 메서드 호출
    val metrics = when {
        input.timeWindowUnit != null -> {
            service.getMetricsByTimeUnit(...)
        }
        input.startTime != null && input.endTime != null -> {
            service.getMetricsForCampaign(...)
        }
        else -> {
            service.getAllMetricsForCampaign(...)
        }
    }

    // 2. 요약 정보 조회
    val summary = service.getCampaignSummary(input.campaignId)

    // 3. DTO 변환 및 반환
    return GetCampaignDashboardUseCaseOut(
        campaignId = input.campaignId,
        metrics = metrics.map { it.toDto() },
        summary = summary.toDto()
    )
}
```

### 5. Presentation Layer

#### `CampaignDashboardController.kt`

**엔드포인트 1: 대시보드 조회**
```kotlin
@GetMapping("/{campaignId}/dashboard")
suspend fun getCampaignDashboard(
    @PathVariable campaignId: Long,
    @RequestParam startTime: LocalDateTime?,
    @RequestParam endTime: LocalDateTime?,
    @RequestParam timeWindowUnit: TimeWindowUnit?
): ApiResponse<GetCampaignDashboardUseCaseOut>
```

**엔드포인트 2: 실시간 스트리밍 (SSE)**
```kotlin
@GetMapping(
    path = ["/{campaignId}/dashboard/stream"],
    produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
)
fun streamCampaignDashboard(
    @PathVariable campaignId: Long,
    @RequestParam durationSeconds: Long = 3600
): Flux<ServerSentEvent<CampaignEventData>>
```

**SSE 이벤트 구조**:
```
event: campaign-event
id: 12345
data: {
  "campaignId": 1,
  "eventId": 12345,
  "userId": 100,
  "eventName": "user_signup",
  "timestamp": "2024-01-15T14:35:23.123"
}

event: campaign-event
id: 12346
data: {...}
```

**엔드포인트 3: 요약 정보**
```kotlin
@GetMapping("/{campaignId}/dashboard/summary")
suspend fun getCampaignSummary(
    @PathVariable campaignId: Long
): ApiResponse<CampaignSummaryResponse>
```

---

## API 명세

### 1. 대시보드 메트릭 조회

#### Request
```http
GET /api/v1/campaigns/{campaignId}/dashboard?startTime={start}&endTime={end}&timeWindowUnit={unit}
```

**Path Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| campaignId | Long | O | 캠페인 ID |

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| startTime | DateTime | X | 조회 시작 시간 (ISO 8601) | 2024-01-01T00:00:00 |
| endTime | DateTime | X | 조회 종료 시간 (ISO 8601) | 2024-01-08T00:00:00 |
| timeWindowUnit | String | X | 시간 단위 (MINUTE/HOUR/DAY/WEEK/MONTH) | HOUR |

**조회 모드**:
1. **전체 조회**: 파라미터 없음 → 모든 메트릭 반환
2. **시간 범위 조회**: startTime + endTime → 해당 범위의 메트릭 반환
3. **시간 단위 조회**: timeWindowUnit → 해당 단위의 최근 메트릭 반환

#### Response
```json
{
  "success": true,
  "data": {
    "campaignId": 1,
    "metrics": [
      {
        "id": 101,
        "campaignId": 1,
        "metricType": "EVENT_COUNT",
        "metricValue": 1523,
        "timeWindowStart": "2024-01-15T14:00:00",
        "timeWindowEnd": "2024-01-15T15:00:00",
        "timeWindowUnit": "HOUR",
        "createdAt": "2024-01-15T14:00:01",
        "updatedAt": "2024-01-15T14:59:58"
      },
      {
        "id": 102,
        "campaignId": 1,
        "metricType": "EVENT_COUNT",
        "metricValue": 35420,
        "timeWindowStart": "2024-01-15T00:00:00",
        "timeWindowEnd": "2024-01-16T00:00:00",
        "timeWindowUnit": "DAY",
        "createdAt": "2024-01-15T00:00:01",
        "updatedAt": "2024-01-15T23:59:59"
      }
    ],
    "summary": {
      "campaignId": 1,
      "totalEvents": 152340,
      "eventsLast24Hours": 35420,
      "eventsLast7Days": 89231,
      "lastUpdated": "2024-01-15T15:00:00"
    }
  }
}
```

### 2. 실시간 스트리밍 (SSE)

#### Request
```http
GET /api/v1/campaigns/{campaignId}/dashboard/stream?durationSeconds={duration}
```

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| durationSeconds | Long | X | 3600 | 스트리밍 지속 시간 (초) |

#### Response (SSE Stream)
```
event: campaign-event
id: 12345
data: {"campaignId":1,"eventId":12345,"userId":100,"eventName":"user_signup","timestamp":"2024-01-15T14:35:23.123"}

event: campaign-event
id: 12346
data: {"campaignId":1,"eventId":12346,"userId":101,"eventName":"purchase","timestamp":"2024-01-15T14:35:25.456"}

event: stream-end
data: Stream ended
```

**클라이언트 예시 (JavaScript)**:
```javascript
const eventSource = new EventSource(
  '/api/v1/campaigns/1/dashboard/stream?durationSeconds=3600'
);

eventSource.addEventListener('campaign-event', (e) => {
  const event = JSON.parse(e.data);
  console.log('New event:', event);
  updateDashboard(event);
});

eventSource.addEventListener('stream-end', () => {
  console.log('Stream completed');
  eventSource.close();
});

eventSource.onerror = (error) => {
  console.error('SSE error:', error);
  eventSource.close();
};
```

### 3. 캠페인 요약 정보

#### Request
```http
GET /api/v1/campaigns/{campaignId}/dashboard/summary
```

#### Response
```json
{
  "success": true,
  "data": {
    "campaignId": 1,
    "totalEvents": 152340,
    "eventsLast24Hours": 35420,
    "eventsLast7Days": 89231,
    "lastUpdated": "2024-01-15T15:00:00"
  }
}
```

---

## 기술적 의사결정

### 1. Redis Stream vs Kafka

**선택**: Redis Stream

**이유**:
- ✅ **인프라 단순성**: 기존 Redis 인프라 재사용 가능
- ✅ **낮은 지연시간**: 메모리 기반으로 밀리초 단위 처리
- ✅ **경량성**: 작은 메시지 크기 (< 1KB)에 적합
- ✅ **컨슈머 그룹 지원**: Kafka와 유사한 분산 처리 가능

**Kafka 대비 단점**:
- ❌ **영속성**: 재시작 시 데이터 손실 가능 (AOF 설정 필요)
- ❌ **처리량**: Kafka 대비 처리량 낮음 (하지만 요구사항에는 충분)
- ❌ **데이터 보관**: 장기 보관에는 부적합

**결론**: 실시간 대시보드용으로는 Redis Stream이 적합. 장기 분석이 필요하면 DB 메트릭 활용.

### 2. DB 메트릭 저장 시점

**선택**: 이벤트 발생 시 즉시 저장

**대안 1**: 배치로 주기적 저장
- 장점: DB 부하 감소
- 단점: 실시간성 저하, 애플리케이션 재시작 시 데이터 손실

**대안 2**: Stream Consumer가 저장
- 장점: PostEventUseCase 성능 개선
- 단점: 아키텍처 복잡도 증가, 데이터 일관성 보장 어려움

**결론**:
- 현재 요구사항에서는 즉시 저장이 적합
- 이벤트 발생률이 높아지면 배치 저장으로 전환 고려

### 3. 시간 윈도우 선택

**선택**: HOUR와 DAY 두 개만 자동 생성

**이유**:
- ✅ **저장 공간 효율**: 모든 단위 생성 시 5배 증가
- ✅ **쿼리 성능**: 적은 레코드 수로 빠른 조회
- ✅ **충분한 정보**: 대부분의 대시보드 요구사항 충족

**확장 방안**:
- MINUTE: 실시간 모니터링이 필요한 캠페인만 선택적 생성
- WEEK/MONTH: 주기적 배치 작업으로 집계

### 4. 동시성 처리 전략

**현재**: Optimistic Approach (낙관적 접근)
```kotlin
val existing = repository.find(...)
if (existing != null) {
    existing.incrementValue(1)
    repository.save(existing)
}
```

**문제점**:
- Race Condition 발생 가능
- 동시 이벤트 발생 시 일부 카운트 누락

**개선 방안 1**: Database Upsert
```kotlin
repository.upsertMetric(
    campaignId = campaignId,
    metricType = metricType,
    incrementBy = 1,
    onDuplicateKey = "metric_value = metric_value + VALUES(increment_value)"
)
```

**개선 방안 2**: Redis 원자적 증가 + 주기적 동기화
```kotlin
// Redis에서 카운트 증가
redis.hincrby("campaign:metrics:${campaignId}:${window}", metricType, 1)

// 주기적으로 DB에 flush (1분마다)
scheduledTask {
    val redisMetrics = redis.hgetall(key)
    repository.batchUpdate(redisMetrics)
}
```

**권장**: 트래픽 증가 시 개선 방안 2 채택

### 5. Reactive vs Imperative

**선택**: Reactive (WebFlux + Coroutines)

**이유**:
- ✅ **SSE 지원**: Flux를 통한 자연스러운 스트리밍
- ✅ **높은 동시성**: 적은 스레드로 많은 요청 처리
- ✅ **Redis Reactive 지원**: ReactiveRedisTemplate 활용
- ✅ **기존 아키텍처 일관성**: 프로젝트 전체가 WebFlux 기반

**주의사항**:
- ⚠️ **학습 곡선**: 팀의 Reactive 경험 필요
- ⚠️ **디버깅**: 스택 트레이스가 복잡함
- ⚠️ **블로킹 코드 주의**: 실수로 블로킹 시 성능 저하

---

## 성능 고려사항

### 1. 데이터베이스 쿼리 성능

#### 쿼리 패턴 분석

**패턴 1**: 특정 캠페인의 특정 시간 범위 조회
```sql
SELECT * FROM campaign_dashboard_metrics
WHERE campaign_id = ?
  AND time_window_start BETWEEN ? AND ?
ORDER BY time_window_start DESC;
```
- 사용 인덱스: `idx_campaign_id_metric_type` (부분 사용) + `idx_time_window`
- 예상 성능: O(log n) - 인덱스 스캔

**패턴 2**: 특정 시간 윈도우 단위 조회
```sql
SELECT * FROM campaign_dashboard_metrics
WHERE campaign_id = ?
  AND time_window_unit = ?
  AND time_window_start > ?
ORDER BY time_window_start DESC;
```
- 사용 인덱스: `unique_campaign_metric_time` (복합 인덱스)
- 예상 성능: O(log n)

**최적화**:
- ✅ 복합 인덱스로 커버링 인덱스 효과
- ✅ UNIQUE 제약으로 중복 방지 및 성능 향상

#### 쿼리 실행 계획 확인 방법

```sql
EXPLAIN SELECT * FROM campaign_dashboard_metrics
WHERE campaign_id = 1
  AND time_window_start BETWEEN '2024-01-01' AND '2024-01-08';
```

**기대 결과**:
```
+----+-------------+---------------------------+-------+---------------+------------------+
| id | select_type | table                     | type  | key           | rows | Extra     |
+----+-------------+---------------------------+-------+---------------+------+-----------+
|  1 | SIMPLE      | campaign_dashboard_metrics| range | idx_time_window| 168 | Using index|
+----+-------------+---------------------------+-------+---------------+------+-----------+
```

### 2. Redis Stream 성능

#### 처리량 벤치마크 (추정)

**단일 Redis 인스턴스**:
- 초당 이벤트 발행: ~10,000 ops/sec
- 초당 이벤트 읽기: ~15,000 ops/sec

**Redis Cluster (3 master)**:
- 초당 이벤트 발행: ~30,000 ops/sec
- 초당 이벤트 읽기: ~45,000 ops/sec

**현재 시스템 요구사항**:
- 캠페인 1,000개
- 평균 100 events/campaign/day = 1.16 events/campaign/sec
- 총 처리량: ~1,200 events/sec

**여유율**: 30,000 / 1,200 = **25배 여유**

#### Stream 크기 관리

**메모리 사용량 계산**:
```
단일 이벤트 크기: ~200 bytes (직렬화 후)
10,000개 이벤트: 2 MB
캠페인 1,000개 × 10,000개: 2 GB
```

**메모리 관리 전략**:
1. **자동 Trim**: 캠페인당 최근 10,000개 이벤트만 유지
2. **TTL 설정**: 7일 이상 오래된 스트림 자동 삭제
3. **모니터링**: Redis 메모리 사용률 알림 (80% 이상)

### 3. API 응답 시간 목표

| 엔드포인트 | 목표 응답 시간 (P95) | 비고 |
|-----------|---------------------|------|
| GET /dashboard | < 100ms | 인덱스 활용 시 |
| GET /dashboard/summary | < 50ms | 집계 쿼리 |
| GET /dashboard/stream | < 10ms (첫 연결) | SSE 연결 설정 |

**측정 방법**:
```kotlin
// Spring Boot Actuator + Micrometer 활용
@Timed(value = "api.campaign.dashboard")
suspend fun getCampaignDashboard(...) {
    // ...
}
```

### 4. 캐싱 전략

#### Level 1: Application Cache (Caffeine)
```kotlin
@Cacheable(value = ["campaignSummary"], key = "#campaignId")
suspend fun getCampaignSummary(campaignId: Long): CampaignDashboardSummary {
    // 5분 TTL로 캐싱
}
```

#### Level 2: Redis Cache
```kotlin
// 이미 구현된 CampaignCacheManager 활용
cacheManager.loadAndSaveIfMiss("campaign::summary::${campaignId}") {
    calculateSummary(campaignId)
}
```

**무효화 전략**:
- 이벤트 발생 시: 해당 캠페인의 summary 캐시 무효화
- 주기적: 5분마다 자동 만료

---

## 보안 및 안정성

### 1. 입력 검증

#### Path Variable 검증
```kotlin
@GetMapping("/{campaignId}/dashboard")
suspend fun getCampaignDashboard(
    @PathVariable @Min(1) campaignId: Long,  // 양수만 허용
    // ...
)
```

#### Query Parameter 검증
```kotlin
// 시간 범위 검증
if (startTime != null && endTime != null) {
    require(startTime.isBefore(endTime)) {
        "startTime must be before endTime"
    }
    require(endTime.isBefore(LocalDateTime.now().plusDays(1))) {
        "endTime cannot be in the future"
    }
}
```

#### Duration 검증
```kotlin
@RequestParam(defaultValue = "3600")
@Min(1) @Max(86400)  // 최대 24시간
durationSeconds: Long
```

### 2. Rate Limiting

**API 호출 제한** (구현 필요):
```kotlin
@RateLimiter(name = "campaignDashboard", fallbackMethod = "rateLimitFallback")
suspend fun getCampaignDashboard(...) {
    // ...
}
```

**권장 설정**:
- 일반 조회 API: 100 req/min/user
- SSE 스트리밍: 5 concurrent connections/user
- Summary API: 200 req/min/user

### 3. 에러 처리

#### Stream 발행 실패 처리
```kotlin
try {
    campaignDashboardService.publishCampaignEvent(dashboardEvent)
} catch (e: Exception) {
    log.error("Failed to publish event", e)
    // 이벤트 저장은 성공했으므로 예외를 삼킴
    // Redis 장애가 이벤트 저장을 막지 않도록
}
```

#### SSE 연결 에러 처리
```kotlin
.onErrorResume { error ->
    log.error("Stream error", error)
    Flux.just(
        ServerSentEvent.builder<CampaignEventData>()
            .event("error")
            .comment(error.message ?: "Stream error")
            .build()
    )
}
```

### 4. 데이터 일관성

#### 메트릭 정합성 검증
```kotlin
// 주기적으로 메트릭 정합성 검사 (배치 작업)
@Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
suspend fun validateMetricsConsistency() {
    campaigns.forEach { campaign ->
        val dbCount = campaignEventsRepository.countByCampaignId(campaign.id)
        val metricsSum = metricsRepository
            .findByCampaignId(campaign.id)
            .sumOf { it.metricValue }

        if (abs(dbCount - metricsSum) > dbCount * 0.01) {  // 1% 이상 차이
            log.warn("Metrics inconsistency detected for campaign ${campaign.id}")
            // 알림 발송 또는 자동 수정
        }
    }
}
```

### 5. 모니터링 및 알림

#### 헬스 체크
```kotlin
@Component
class CampaignDashboardHealthIndicator(
    private val streamService: CampaignDashboardStreamService
) : HealthIndicator {
    override fun health(): Health {
        return try {
            // Redis Stream 연결 확인
            streamService.getStreamLength(testCampaignId)
            Health.up().build()
        } catch (e: Exception) {
            Health.down()
                .withException(e)
                .build()
        }
    }
}
```

#### 메트릭 수집
```kotlin
// Micrometer 메트릭
registry.gauge("campaign.dashboard.stream.size", streamSize)
registry.counter("campaign.dashboard.events.published").increment()
registry.timer("campaign.dashboard.metrics.calculation").record(duration)
```

---

## 테스트 전략

### 1. 단위 테스트

#### Service Layer 테스트
```kotlin
class CampaignDashboardServiceTest : BehaviorSpec({
    given("publishCampaignEvent") {
        `when`("이벤트를 발행하면") {
            then("Redis Stream에 발행되어야 함") {
                coVerify { streamService.publishEvent(any()) }
            }
            then("시간별, 일별 메트릭이 생성되어야 함") {
                coVerify(exactly = 2) {
                    metricsRepository.save(any())
                }
            }
        }
    }

    given("calculateTimeWindow") {
        `when`("HOUR 단위로 계산하면") {
            val result = service.calculateTimeWindow(
                LocalDateTime.of(2024, 1, 15, 14, 35, 23),
                TimeWindowUnit.HOUR
            )
            then("정확한 시간 경계를 반환해야 함") {
                result.first shouldBe LocalDateTime.of(2024, 1, 15, 14, 0, 0)
                result.second shouldBe LocalDateTime.of(2024, 1, 15, 15, 0, 0)
            }
        }
    }
})
```

### 2. 통합 테스트

#### Repository 테스트
```kotlin
@DataR2dbcTest
class CampaignDashboardMetricsRepositoryTest {
    @Test
    fun `시간 범위로 메트릭 조회`() = runTest {
        // given
        val campaign = campaignRepository.save(Campaign(...))
        val metrics = listOf(
            CampaignDashboardMetrics.new(...),
            CampaignDashboardMetrics.new(...)
        )
        metricsRepository.saveAll(metrics).collect()

        // when
        val result = metricsRepository
            .findByCampaignIdAndTimeWindowStartBetween(
                campaign.id!!,
                startTime,
                endTime
            )
            .toList()

        // then
        result.size shouldBe 2
    }
}
```

#### Redis Stream 테스트
```kotlin
@SpringBootTest
@Testcontainers
class CampaignDashboardStreamServiceIntegrationTest {
    @Container
    val redis = GenericContainer<Nothing>("redis:7-alpine")
        .apply { withExposedPorts(6379) }

    @Test
    fun `이벤트 발행 및 구독 테스트`() = runTest {
        // given
        val event = CampaignDashboardEvent(...)

        // when
        streamService.publishEvent(event)

        // then
        val received = streamService.streamEvents(campaignId)
            .take(1)
            .awaitFirst()

        received.eventId shouldBe event.eventId
    }
}
```

### 3. 성능 테스트

#### JMeter 시나리오
```xml
<!-- 100명의 동시 사용자가 1분간 대시보드 조회 -->
<ThreadGroup>
  <numThreads>100</numThreads>
  <rampTime>10</rampTime>
  <duration>60</duration>
</ThreadGroup>
```

#### Gatling 시나리오
```scala
val scn = scenario("Dashboard Load Test")
  .exec(
    http("Get Dashboard")
      .get("/api/v1/campaigns/1/dashboard")
      .check(status.is(200))
      .check(jsonPath("$.success").is("true"))
  )

setUp(
  scn.inject(
    rampUsers(100) during (10.seconds),
    constantUsersPerSec(50) during (60.seconds)
  )
)
```

**목표 성능**:
- 평균 응답 시간: < 100ms
- P95 응답 시간: < 200ms
- P99 응답 시간: < 500ms
- 에러율: < 0.1%

---

## 운영 가이드

### 1. 배포 절차

#### 데이터베이스 마이그레이션
```bash
# Flyway 마이그레이션 실행
./gradlew flywayMigrate

# 마이그레이션 확인
./gradlew flywayInfo
```

#### 애플리케이션 배포
```bash
# 빌드
./gradlew build

# Docker 이미지 생성
docker build -t crm-backend:latest .

# 배포 (Blue-Green 방식)
kubectl apply -f k8s/deployment-green.yaml
kubectl set image deployment/crm-backend crm-backend=crm-backend:latest

# 헬스 체크 확인
kubectl rollout status deployment/crm-backend
```

### 2. 모니터링 지표

#### Application Metrics
```
# 대시보드 API 호출 수
campaign_dashboard_requests_total{endpoint="/dashboard"}

# 평균 응답 시간
campaign_dashboard_request_duration_seconds{quantile="0.95"}

# Redis Stream 크기
campaign_dashboard_stream_size{campaign_id="1"}

# 메트릭 업데이트 지연
campaign_dashboard_metrics_delay_seconds
```

#### Infrastructure Metrics
```
# Redis 메모리 사용률
redis_memory_used_percent

# MySQL 연결 풀
hikaricp_connections_active

# JVM 힙 사용률
jvm_memory_used_bytes{area="heap"}
```

#### Business Metrics
```
# 활성 캠페인 수
active_campaigns_count

# 시간당 이벤트 발생 수
campaign_events_per_hour

# SSE 연결 수
campaign_dashboard_sse_connections_active
```

### 3. 장애 대응

#### Redis 장애 시
```kotlin
// Circuit Breaker 설정 (Resilience4j)
@CircuitBreaker(name = "redisStream", fallbackMethod = "fallbackPublish")
suspend fun publishEvent(event: CampaignDashboardEvent) {
    streamService.publishEvent(event)
}

fun fallbackPublish(event: CampaignDashboardEvent, ex: Exception) {
    log.error("Redis unavailable, event not published", ex)
    // 메트릭은 DB에 저장되므로 데이터 손실 없음
    // 실시간 스트리밍만 중단됨
}
```

**대응 절차**:
1. Redis 재시작 시도
2. 클러스터 상태 확인
3. 필요 시 슬레이브 승격
4. 애플리케이션은 계속 동작 (degraded mode)

#### 데이터베이스 장애 시
```
1. Read Replica로 자동 페일오버 (R2DBC 설정 활용)
2. Master 복구 시도
3. 메트릭 데이터 정합성 검사
4. 필요 시 Redis Stream에서 재집계
```

#### 메트릭 불일치 발견 시
```bash
# 수동 재집계 스크립트
./scripts/recalculate-metrics.sh --campaign-id=1 --start-date=2024-01-01
```

### 4. 백업 및 복구

#### Redis Stream 백업
```bash
# AOF 활성화 (redis.conf)
appendonly yes
appendfsync everysec

# RDB 스냅샷 (주기적)
redis-cli BGSAVE
```

#### 데이터베이스 백업
```bash
# 일일 백업 (mysqldump)
mysqldump -u root -p crm_db campaign_dashboard_metrics > backup_$(date +%Y%m%d).sql

# Point-in-time Recovery 설정
# binlog 활성화 및 보관 기간 설정 (7일)
```

### 5. 성능 튜닝 체크리스트

- [ ] 데이터베이스 인덱스 사용률 확인 (`EXPLAIN` 분석)
- [ ] Redis 메모리 사용률 < 80% 유지
- [ ] Connection Pool 크기 최적화
- [ ] JVM 힙 크기 조정 (-Xmx, -Xms)
- [ ] GC 로그 분석 및 튜닝
- [ ] Slow Query 로그 모니터링
- [ ] API 응답 시간 P99 < 500ms 유지

---

## 개선 방향

### 1. 단기 개선 (1-2개월)

#### 1.1 타임존 지원
**문제**: 현재 시스템 타임존만 지원
```kotlin
// 개선안
data class GetCampaignDashboardUseCaseIn(
    val campaignId: Long,
    val timeZone: ZoneId = ZoneId.of("Asia/Seoul"),  // 기본값
    // ...
)

private fun calculateTimeWindow(
    timestamp: ZonedDateTime,  // LocalDateTime → ZonedDateTime
    unit: TimeWindowUnit
): Pair<ZonedDateTime, ZonedDateTime>
```

#### 1.2 메트릭 타입 확장
```kotlin
enum class MetricType {
    EVENT_COUNT,              // 기존
    UNIQUE_USER_COUNT,        // 신규: 고유 사용자 수
    CONVERSION_RATE,          // 신규: 전환율
    AVERAGE_EVENT_VALUE,      // 신규: 평균 이벤트 값
    CLICK_THROUGH_RATE        // 신규: 클릭률
}
```

#### 1.3 실시간 집계 최적화
```kotlin
// Redis에 임시 카운터 저장 → 주기적으로 DB 동기화
@Scheduled(fixedDelay = 60000)  // 1분마다
suspend fun flushMetricsToDatabase() {
    val pendingMetrics = redis.hgetall("pending:metrics:*")
    metricsRepository.batchUpsert(pendingMetrics)
    redis.delete("pending:metrics:*")
}
```

### 2. 중기 개선 (3-6개월)

#### 2.1 대시보드 프론트엔드 구현
```typescript
// React + TypeScript + Recharts
import { LineChart, Line, XAxis, YAxis } from 'recharts';

function CampaignDashboard({ campaignId }: Props) {
  const [metrics, setMetrics] = useState<Metric[]>([]);

  useEffect(() => {
    // SSE 연결
    const eventSource = new EventSource(
      `/api/v1/campaigns/${campaignId}/dashboard/stream`
    );

    eventSource.addEventListener('campaign-event', (e) => {
      const event = JSON.parse(e.data);
      updateMetrics(event);
    });

    return () => eventSource.close();
  }, [campaignId]);

  return (
    <LineChart data={metrics}>
      <Line dataKey="metricValue" />
      <XAxis dataKey="timeWindowStart" />
      <YAxis />
    </LineChart>
  );
}
```

#### 2.2 알림 기능
```kotlin
// 임계값 기반 알림
@Component
class CampaignMetricAlertService {
    suspend fun checkThresholds(metric: CampaignDashboardMetrics) {
        val threshold = getThreshold(metric.campaignId, metric.metricType)

        if (metric.metricValue > threshold) {
            sendAlert(
                title = "캠페인 메트릭 임계값 초과",
                message = "캠페인 ${metric.campaignId}의 ${metric.metricType} " +
                         "값이 ${metric.metricValue}로 임계값 ${threshold}를 초과했습니다.",
                severity = AlertSeverity.WARNING
            )
        }
    }
}
```

#### 2.3 A/B 테스트 지원
```kotlin
// 캠페인 변형별 메트릭 비교
data class CampaignVariant(
    val campaignId: Long,
    val variantName: String,  // "control", "variant_a", "variant_b"
    val trafficAllocation: Double  // 0.0 ~ 1.0
)

suspend fun compareVariants(
    campaignId: Long,
    startTime: LocalDateTime,
    endTime: LocalDateTime
): List<VariantMetrics>
```

### 3. 장기 개선 (6개월 이상)

#### 3.1 머신러닝 기반 예측
```kotlin
// 캠페인 성과 예측
@Service
class CampaignPredictionService {
    suspend fun predictMetrics(
        campaignId: Long,
        forecastHorizon: Duration
    ): List<PredictedMetric> {
        val historicalData = metricsRepository
            .findByCampaignId(campaignId)
            .toList()

        // Python ML 서비스 호출 (gRPC or REST)
        return mlService.forecast(historicalData, forecastHorizon)
    }
}
```

#### 3.2 이상 탐지
```kotlin
// 통계적 이상 탐지 (Z-score)
suspend fun detectAnomalies(campaignId: Long): List<AnomalyEvent> {
    val metrics = getRecentMetrics(campaignId, last30Days)
    val mean = metrics.map { it.metricValue }.average()
    val stdDev = calculateStdDev(metrics)

    return metrics.filter { metric ->
        val zScore = abs((metric.metricValue - mean) / stdDev)
        zScore > 3.0  // 3-sigma 이상 이탈
    }.map { AnomalyEvent(it, "Unusual spike detected") }
}
```

#### 3.3 멀티 리전 지원
```yaml
# 리전별 Redis Cluster
regions:
  - name: ap-northeast-2
    redis:
      nodes: [seoul-redis-1, seoul-redis-2, seoul-redis-3]
  - name: us-west-2
    redis:
      nodes: [oregon-redis-1, oregon-redis-2, oregon-redis-3]

# 지역 기반 라우팅
routing:
  strategy: latency-based
```

### 4. 기술 부채 해결

#### 4.1 Race Condition 해결
```kotlin
// Database Upsert 구현
@Modifying
@Query("""
    INSERT INTO campaign_dashboard_metrics
    (campaign_id, metric_type, metric_value, time_window_start, time_window_end, time_window_unit)
    VALUES (:campaignId, :metricType, :metricValue, :start, :end, :unit)
    ON DUPLICATE KEY UPDATE
        metric_value = metric_value + VALUES(metric_value),
        updated_at = CURRENT_TIMESTAMP(6)
""")
suspend fun upsertMetric(/* parameters */)
```

#### 4.2 테스트 커버리지 향상
```
현재: ~60% (추정)
목표: 80% 이상

- Unit Test: 모든 Service, UseCase
- Integration Test: Repository, Stream Service
- E2E Test: 주요 API 플로우
```

#### 4.3 문서화 개선
- [ ] OpenAPI 3.0 스펙 자동 생성
- [ ] 아키텍처 다이어그램 (C4 Model)
- [ ] API 사용 예제 및 튜토리얼
- [ ] 운영 런북 (Runbook)

---

## 부록

### A. Redis Stream 커맨드 참고

```bash
# 스트림에 이벤트 추가
XADD campaign:dashboard:stream:1 * campaignId 1 eventId 100 userId 50

# 스트림 읽기 (처음부터)
XREAD STREAMS campaign:dashboard:stream:1 0

# 스트림 길이 확인
XLEN campaign:dashboard:stream:1

# 스트림 Trim (최근 10000개만 유지)
XTRIM campaign:dashboard:stream:1 MAXLEN ~ 10000

# Consumer Group 생성
XGROUP CREATE campaign:dashboard:stream:1 dashboard-aggregator 0

# Consumer Group에서 읽기
XREADGROUP GROUP dashboard-aggregator consumer-1 STREAMS campaign:dashboard:stream:1 >
```

### B. 유용한 쿼리 모음

```sql
-- 시간대별 이벤트 트렌드
SELECT
    DATE_FORMAT(time_window_start, '%Y-%m-%d %H:00:00') AS hour,
    SUM(metric_value) AS total_events
FROM campaign_dashboard_metrics
WHERE campaign_id = 1
  AND metric_type = 'EVENT_COUNT'
  AND time_window_unit = 'HOUR'
  AND time_window_start >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
GROUP BY hour
ORDER BY hour;

-- 캠페인별 성과 비교
SELECT
    c.id,
    c.name,
    SUM(CASE WHEN m.time_window_unit = 'DAY' THEN m.metric_value ELSE 0 END) AS daily_events
FROM campaigns c
LEFT JOIN campaign_dashboard_metrics m ON c.id = m.campaign_id
WHERE m.time_window_start >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY c.id, c.name
ORDER BY daily_events DESC
LIMIT 10;

-- 메트릭 정합성 검사
SELECT
    m.campaign_id,
    SUM(m.metric_value) AS metrics_total,
    COUNT(DISTINCT ce.event_id) AS actual_events,
    ABS(SUM(m.metric_value) - COUNT(DISTINCT ce.event_id)) AS difference
FROM campaign_dashboard_metrics m
LEFT JOIN campaign_events ce ON m.campaign_id = ce.campaign_id
WHERE m.metric_type = 'EVENT_COUNT'
GROUP BY m.campaign_id
HAVING difference > 0;
```

### C. 트러블슈팅 가이드

| 증상 | 원인 | 해결 방법 |
|-----|------|----------|
| SSE 연결이 자주 끊김 | Nginx 타임아웃 | `proxy_read_timeout 3600s;` 설정 |
| 메트릭 값이 실제보다 작음 | Race Condition | Upsert 쿼리로 변경 |
| Redis 메모리 부족 | Stream 크기 제한 없음 | `XTRIM` 주기적 실행 |
| API 응답 느림 | 인덱스 미사용 | `EXPLAIN` 분석 후 인덱스 추가 |
| 메트릭 중복 생성 | UNIQUE 제약 누락 | 마이그레이션 재실행 |

---

## 결론

본 구현은 Redis Stream과 시간 윈도우 기반 집계를 활용하여 **확장 가능하고 실시간성을 보장하는** 캠페인 대시보드 시스템을 제공합니다.

### 핵심 성과
✅ **실시간 이벤트 스트리밍**: SSE를 통한 밀리초 단위 업데이트
✅ **효율적인 데이터 집계**: 시간 윈도우 단위 사전 집계로 쿼리 성능 보장
✅ **확장 가능한 아키텍처**: Redis Cluster와 Reactive Stack 활용
✅ **운영 편의성**: 자동 메트릭 생성 및 모니터링 지원

### 향후 과제
- [ ] 타임존 지원 추가
- [ ] Race Condition 완전 해결 (Upsert)
- [ ] 프론트엔드 대시보드 개발
- [ ] ML 기반 예측 및 이상 탐지
- [ ] 멀티 리전 확장

**문의**: [기술 팀 이메일] 또는 GitHub Issues
