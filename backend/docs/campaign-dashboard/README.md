# Campaign Dashboard - 실시간 스트리밍 대시보드

## 📋 목차
1. [개요](#개요)
2. [아키텍처](#아키텍처)
3. [주요 컴포넌트](#주요-컴포넌트)
4. [API 엔드포인트](#api-엔드포인트)
5. [사용 예시](#사용-예시)
6. [설정 및 제약사항](#설정-및-제약사항)

---

## 개요

Campaign Dashboard는 캠페인의 실시간 이벤트를 추적하고 집계된 메트릭을 제공하는 기능입니다.

### 주요 기능
- ✅ **실시간 이벤트 스트리밍** (Redis Stream + SSE)
- ✅ **메트릭 집계** (시간 단위별 이벤트 카운트)
- ✅ **요약 통계** (전체/24시간/7일)
- ✅ **자동 메모리 관리** (Stream trim)
- ✅ **모니터링** (Stream 상태 조회)

### 기술 스택
- **Storage**: Redis Stream (실시간), PostgreSQL (집계 메트릭)
- **Framework**: Spring WebFlux (Reactive)
- **Protocol**: Server-Sent Events (SSE)

---

## 아키텍처

```
┌─────────────┐
│   Event     │
│  발생       │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────┐
│     PostEventUseCase                    │
│  - 이벤트 저장                           │
│  - publishCampaignEvent() 호출           │
└──────┬──────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│  CampaignDashboardService               │
│  - Redis Stream 발행                     │
│  - DB 메트릭 업데이트                    │
│  - 자동 trim (100개마다)                 │
└──────┬──────────────────────────────────┘
       │
       ├──────────────────┬─────────────────┐
       ▼                  ▼                 ▼
┌─────────────┐  ┌──────────────┐  ┌──────────────┐
│Redis Stream │  │  PostgreSQL  │  │ SSE Clients  │
│(실시간)     │  │  (집계)      │  │ (구독자)     │
└─────────────┘  └──────────────┘  └──────────────┘
```

---

## 주요 컴포넌트

### 1️⃣ Domain Layer

#### CampaignDashboardEvent
```kotlin
data class CampaignDashboardEvent(
    val campaignId: Long,
    val eventId: Long,
    val userId: Long,
    val eventName: String,
    val timestamp: LocalDateTime
)
```
- Redis Stream에 발행되는 실시간 이벤트

#### CampaignDashboardMetrics
```kotlin
class CampaignDashboardMetrics(
    val campaignId: Long,
    val metricType: MetricType,          // EVENT_COUNT
    val metricValue: Long,               // 해당 시간 윈도우의 집계 값
    val timeWindowStart: LocalDateTime,
    val timeWindowEnd: LocalDateTime,
    val timeWindowUnit: TimeWindowUnit   // MINUTE, HOUR, DAY, WEEK, MONTH
)
```
- PostgreSQL에 저장되는 집계 메트릭

**metricValue의 의미** (MetricType별)
- `EVENT_COUNT`: 시간 윈도우 내 이벤트 발생 횟수 (예: 1,250개)
- `UNIQUE_USER_COUNT`: 고유 사용자 수 (미구현)
- `TOTAL_USER_COUNT`: 전체 사용자 수 (미구현)

**예시**:
```json
{
  "metricType": "EVENT_COUNT",
  "metricValue": 1250,                 // 13시~14시에 1,250개 이벤트 발생
  "timeWindowStart": "2025-11-16T13:00:00",
  "timeWindowEnd": "2025-11-16T14:00:00",
  "timeWindowUnit": "HOUR"
}
```

### 2️⃣ Service Layer

#### CampaignDashboardStreamService
**역할**: Redis Stream 관리
- `publishEvent()`: 이벤트 발행
- `streamEvents()`: 실시간 스트리밍
- `getStreamLength()`: Stream 길이 조회
- `trimStream()`: 메모리 관리

**Stream Key 형식**
```
campaign:dashboard:stream:{campaignId}
```

#### CampaignDashboardService
**역할**: 비즈니스 로직 및 집계
- `publishCampaignEvent()`: 이벤트 발행 + DB 저장 + 자동 trim
- `getMetricsForCampaign()`: 시간 범위별 메트릭 조회
- `getCampaignSummary()`: 요약 통계
- `streamCampaignEvents()`: 실시간 스트리밍 (SSE)

**자동 Trim 정책**
- 100개 이벤트마다 실행
- 최대 10,000개 이벤트 유지

### 3️⃣ Application Layer (UseCase)

#### GetCampaignDashboardUseCase
- 시간 범위/단위별 메트릭 + 요약 정보 조회

#### GetCampaignSummaryUseCase
- 캠페인 요약 통계 조회

#### GetStreamStatusUseCase
- Redis Stream 상태 모니터링

### 4️⃣ Controller Layer

#### CampaignDashboardController
- `/api/v1/campaigns/{campaignId}/dashboard/**`
- 모든 엔드포인트는 UseCase 패턴 사용

---

## API 엔드포인트

### 1. 캠페인 대시보드 조회
```http
GET /api/v1/campaigns/{campaignId}/dashboard
```

**Query Parameters:**
- `startTime` (optional): 조회 시작 시간 (ISO 8601)
- `endTime` (optional): 조회 종료 시간 (ISO 8601)
- `timeWindowUnit` (optional): MINUTE, HOUR, DAY, WEEK, MONTH

**Response:**
```json
{
  "success": true,
  "data": {
    "campaignId": 1,
    "metrics": [
      {
        "id": 100,
        "campaignId": 1,
        "metricType": "EVENT_COUNT",
        "metricValue": 1250,
        "timeWindowStart": "2025-11-16T13:00:00",
        "timeWindowEnd": "2025-11-16T14:00:00",
        "timeWindowUnit": "HOUR"
      }
    ],
    "summary": {
      "campaignId": 1,
      "totalEvents": 5000,
      "eventsLast24Hours": 1200,
      "eventsLast7Days": 3500,
      "lastUpdated": "2025-11-16T14:30:00"
    }
  }
}
```

### 2. 실시간 스트리밍 (SSE)
```http
GET /api/v1/campaigns/{campaignId}/dashboard/stream
```

**Query Parameters:**
- `durationSeconds` (optional, default: 3600): 스트리밍 지속 시간 (초)

**Response (SSE):**
```
event: campaign-event
id: 100
data: {"campaignId":1,"eventId":100,"userId":50,"eventName":"click","timestamp":"2025-11-16T14:30:00"}

event: campaign-event
id: 101
data: {"campaignId":1,"eventId":101,"userId":51,"eventName":"view","timestamp":"2025-11-16T14:30:05"}

event: stream-end
data: Stream ended
```

### 3. 캠페인 요약 정보 조회
```http
GET /api/v1/campaigns/{campaignId}/dashboard/summary
```

**Response:**
```json
{
  "success": true,
  "data": {
    "campaignId": 1,
    "totalEvents": 5000,
    "eventsLast24Hours": 1200,
    "eventsLast7Days": 3500,
    "lastUpdated": "2025-11-16T14:30:00"
  }
}
```

### 4. 스트림 상태 조회
```http
GET /api/v1/campaigns/{campaignId}/dashboard/stream/status
```

**Response:**
```json
{
  "success": true,
  "data": {
    "campaignId": 1,
    "streamLength": 350,
    "checkedAt": "2025-11-16T14:30:00"
  }
}
```

---

## 사용 예시

### 1️⃣ 이벤트 발행 → 자동 스트리밍
```bash
# 1. 이벤트 발행
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "user_click",
    "campaignName": "summer-sale",
    "externalId": "user123",
    "properties": [
      {"key": "button", "value": "signup"}
    ]
  }'

# 2. 실시간 스트리밍 시작 (다른 터미널)
curl -N http://localhost:8080/api/v1/campaigns/1/dashboard/stream

# 3. 이벤트 추가 발행하면 스트리밍 터미널에서 실시간 확인 가능
```

### 2️⃣ 대시보드 조회
```bash
# 전체 메트릭 조회
curl http://localhost:8080/api/v1/campaigns/1/dashboard

# 시간 범위 지정
curl "http://localhost:8080/api/v1/campaigns/1/dashboard?startTime=2025-11-16T00:00:00&endTime=2025-11-16T23:59:59"

# 시간 단위 지정 (시간별 집계)
curl "http://localhost:8080/api/v1/campaigns/1/dashboard?timeWindowUnit=HOUR"
```

### 3️⃣ 모니터링
```bash
# 요약 정보
curl http://localhost:8080/api/v1/campaigns/1/dashboard/summary

# Stream 상태
curl http://localhost:8080/api/v1/campaigns/1/dashboard/stream/status
```

---

## 설정 및 제약사항

### Redis Stream 설정
- **Stream Key 패턴**: `campaign:dashboard:stream:{campaignId}`
- **최대 길이**: 10,000 이벤트
- **Trim 주기**: 100개 이벤트마다
- **데이터 형식**: String (JSON 직렬화)

### 메트릭 집계
- **집계 단위**: MINUTE, HOUR, DAY, WEEK, MONTH
- **저장소**: PostgreSQL (`campaign_dashboard_metrics`)
- **자동 생성**: 이벤트 발행 시 HOUR, DAY 단위 자동 생성

### SSE 스트리밍
- **기본 지속 시간**: 1시간 (3600초)
- **최대 지속 시간**: 사용자 정의 가능
- **이벤트 타입**: `campaign-event`, `error`, `stream-end`
- **재연결**: 클라이언트 측에서 처리 필요

### 성능 고려사항
- ✅ 이벤트 발행은 **비동기** 처리 (실패 시에도 메인 플로우 영향 없음)
- ✅ Stream trim은 **100개마다** 실행 (성능 최적화)
- ✅ SSE는 **Reactive Stream** 사용 (논블로킹)
- ⚠️ 대량 트래픽 시 Redis 메모리 모니터링 필요

### 에러 처리
- Stream 발행 실패 → 로그만 남기고 계속 진행
- SSE 연결 실패 → `error` 이벤트 전송 후 종료
- Metric 저장 실패 → 예외 발생 (트랜잭션 롤백)

---

## 향후 개선 사항

### 현재 미구현 기능
- [ ] **Consumer Group 기반 병렬 처리** ([상세 설명](CONSUMER_GROUP.md))
  - 현재: 모든 클라이언트가 모든 이벤트 수신 (Broadcast)
  - 향후: 작업 분산 및 병렬 처리 지원
  - 사용 사례: 이메일 발송, 실시간 집계 Worker
- [ ] 특정 시점부터 스트리밍 (`streamEventsFromTimestamp`)
- [ ] 메트릭 타입 확장 (현재는 EVENT_COUNT만)
- [ ] 배치 집계 작업 (과거 데이터 재집계)

### 추천 개선 사항
- [ ] Redis Cluster 설정 (고가용성)
- [ ] Grafana 대시보드 연동
- [ ] Alert 설정 (Stream 길이 임계값)
- [ ] 이벤트 압축 (저장 공간 최적화)
