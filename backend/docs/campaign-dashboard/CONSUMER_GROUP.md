# Consumer Group - 개념 및 설계 결정

## 📌 현재 상태

**Consumer Group: ❌ 미구현** (제거됨)

```kotlin
// 제거된 코드
companion object {
    private const val CONSUMER_GROUP = "dashboard-aggregator"  // ← 제거됨
    private const val CONSUMER_NAME = "aggregator-1"          // ← 제거됨
}

suspend fun createConsumerGroup(campaignId: Long) { ... }  // ← 제거됨
```

---

## 🤔 Consumer Group이란?

### 정의
**Consumer Group**은 Redis Stream에서 **여러 소비자(Consumer)가 협력해서 메시지를 분산 처리**할 수 있게 해주는 기능입니다.

### 비유
- **Consumer Group 없음** = TV 방송 (모든 시청자가 같은 내용 시청)
- **Consumer Group 사용** = 택배 배송 (각 택배는 1명의 배달원만 배송)

---

## 🔄 동작 방식 비교

### 1️⃣ Consumer Group 없이 (현재 구현)

```
Redis Stream: [Event1, Event2, Event3, Event4, Event5]
              ↓       ↓       ↓       ↓       ↓

Client A:   Event1, Event2, Event3, Event4, Event5  (모두 수신)
Client B:   Event1, Event2, Event3, Event4, Event5  (모두 수신, 중복!)
Client C:   Event1, Event2, Event3, Event4, Event5  (모두 수신, 중복!)
```

**특징**:
- ✅ 모든 클라이언트가 모든 이벤트 수신 (Broadcast)
- ✅ 실시간 모니터링에 적합
- ❌ 작업 분산 불가

**코드**:
```kotlin
// 현재 구현
reactiveRedisTemplate.opsForStream<String, Any>()
    .read(StreamOffset.fromStart(streamKey))  // 처음부터 모두 읽음
    .map { record -> mapRecordToEvent(record) }
```

### 2️⃣ Consumer Group 사용 시

```
Redis Stream: [Event1, Event2, Event3, Event4, Event5]
                 ↓              ↓              ↓

Consumer Group: "dashboard-aggregator"
              ↙        ↓        ↘

Worker A:   Event1, Event4           (2개 처리)
Worker B:   Event2, Event5           (2개 처리)
Worker C:   Event3                   (1개 처리)
```

**특징**:
- ✅ 각 이벤트를 1번만 처리 (No Duplication)
- ✅ 부하 분산 (Load Balancing)
- ✅ 장애 복구 (ACK 메커니즘)
- ❌ 모든 클라이언트가 같은 이벤트를 보지 못함

**코드 (미구현)**:
```kotlin
// 향후 구현 예정
reactiveRedisTemplate.opsForStream<String, Any>()
    .read(
        Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),  // Consumer Group 사용
        StreamOffset.create(streamKey, ReadOffset.lastConsumed())
    )
    .map { record ->
        // 각 이벤트를 1명의 Consumer만 처리
        mapRecordToEvent(record)
    }
```

---

## 💡 실제 시나리오

### 시나리오 1: 대시보드 모니터링 (현재 구현)

**요구사항**: 3명의 관리자가 동시에 같은 대시보드를 보고 싶음

```bash
# 터미널 1 - 관리자 A
curl -N http://localhost:8080/api/v1/campaigns/1/dashboard/stream

# 터미널 2 - 관리자 B
curl -N http://localhost:8080/api/v1/campaigns/1/dashboard/stream

# 터미널 3 - 관리자 C
curl -N http://localhost:8080/api/v1/campaigns/1/dashboard/stream

# 이벤트 발생
POST /api/v1/events { "name": "user_click" }
```

**결과 (Consumer Group 없음 - 현재)**:
```
터미널 1 (관리자 A): event: campaign-event, id: 100  ✅
터미널 2 (관리자 B): event: campaign-event, id: 100  ✅
터미널 3 (관리자 C): event: campaign-event, id: 100  ✅
```
→ **모든 관리자가 같은 이벤트를 봄** (정상 동작 ✅)

**결과 (만약 Consumer Group 사용했다면)**:
```
터미널 1 (관리자 A): event: campaign-event, id: 100  ✅
터미널 2 (관리자 B): (아무것도 안 받음)            ❌
터미널 3 (관리자 C): (아무것도 안 받음)            ❌
```
→ **한 명만 이벤트를 봄** (대시보드 용도로는 부적절 ❌)

---

### 시나리오 2: 이메일 발송 (Consumer Group 필요)

**요구사항**: 이벤트 발생 시 자동으로 이메일 발송

```kotlin
// ❌ Consumer Group 없이 - 문제 발생
@Service
class EmailSender {
    suspend fun start() {
        streamEvents(campaignId).collect { event ->
            sendEmail(event.userId, "New event: ${event.eventName}")
        }
    }
}

// 3개 서버에서 동시 실행하면?
Server 1: sendEmail(user123)  → 이메일 발송 ✅
Server 2: sendEmail(user123)  → 중복 발송! ❌
Server 3: sendEmail(user123)  → 중복 발송! ❌
```
→ **같은 사용자에게 이메일 3통 발송** (문제!)

```kotlin
// ✅ Consumer Group 사용 - 정상 동작
@Service
class EmailSender {
    suspend fun start() {
        streamEventsWithConsumerGroup(
            campaignId = campaignId,
            group = "email-sender",
            consumer = "server-${instanceId}"
        ).collect { event ->
            sendEmail(event.userId, "New event: ${event.eventName}")
        }
    }
}

// 3개 서버에서 동시 실행해도
Server 1: sendEmail(user123)  → 이메일 발송 ✅
Server 2: (다른 이벤트 처리)
Server 3: (다른 이벤트 처리)
```
→ **각 이벤트는 1개 서버만 처리** (정상 ✅)

---

## ❓ 왜 제거했나?

### 의사결정 과정

#### Step 1: 현재 사용 패턴 분석
```kotlin
// CampaignDashboardController.kt
fun streamCampaignDashboard(...): Flux<ServerSentEvent<...>> {
    return campaignDashboardService.streamCampaignEvents(campaignId)
        .map { event -> ServerSentEvent.builder()... }
}
```

**분석 결과**:
- ✅ 목적: 실시간 모니터링 (SSE)
- ✅ 요구사항: 여러 클라이언트가 **동일한 이벤트**를 봐야 함
- ❌ 작업 분산 필요 없음

#### Step 2: Consumer Group 필요성 판단

| 질문 | 답변 | Consumer Group 필요? |
|------|------|---------------------|
| 각 이벤트를 정확히 1번만 처리해야 하나? | ❌ (모든 클라이언트가 봐야 함) | ❌ |
| 병렬 처리로 성능 향상이 필요한가? | ❌ (SSE는 읽기만 함) | ❌ |
| 작업 분산이 필요한가? | ❌ (단순 모니터링) | ❌ |
| 장애 복구(ACK)가 필요한가? | ❌ (일회성 표시) | ❌ |

**결론**: Consumer Group 불필요 → 제거

#### Step 3: 코드 단순화
```kotlin
// Before (복잡)
companion object {
    private const val CONSUMER_GROUP = "dashboard-aggregator"
    private const val CONSUMER_NAME = "aggregator-1"
}

suspend fun createConsumerGroup(campaignId: Long) { ... }

// After (단순)
companion object {
    private const val STREAM_KEY_PREFIX = "campaign:dashboard:stream"
}
```

---

## 📊 사용 사례별 가이드

### Case 1: 모니터링 / 대시보드 / 알림 표시
**요구사항**: 모든 사용자가 같은 정보를 봐야 함

**솔루션**: Consumer Group **불필요** ❌

**예시**:
- 실시간 대시보드 (현재 구현)
- 실시간 차트
- 전광판
- 알림 표시

**구현**:
```kotlin
reactiveRedisTemplate.opsForStream<String, Any>()
    .read(StreamOffset.fromStart(streamKey))  // 모두 읽기
```

---

### Case 2: 작업 처리 / 배치 / 알림 발송
**요구사항**: 각 이벤트를 정확히 1번만 처리

**솔루션**: Consumer Group **필요** ✅

**예시**:
- 이메일 발송
- SMS 전송
- 푸시 알림
- 데이터 변환
- 외부 API 호출

**구현 (향후)**:
```kotlin
reactiveRedisTemplate.opsForStream<String, Any>()
    .read(
        Consumer.from(GROUP, CONSUMER_NAME),
        StreamOffset.create(streamKey, ReadOffset.lastConsumed())
    )
```

---

## 🔮 향후 구현 계획

### Phase 1: Consumer Group 인프라 구축

**새로운 메서드 추가**:
```kotlin
@Service
class CampaignDashboardStreamService {

    // 기존 메서드 (Broadcast용)
    fun streamEvents(campaignId: Long): Flux<CampaignDashboardEvent> {
        // 모든 클라이언트가 모든 이벤트 수신
    }

    // 🆕 새로운 메서드 (작업 분산용)
    fun streamEventsWithConsumerGroup(
        campaignId: Long,
        groupName: String,
        consumerName: String
    ): Flux<CampaignDashboardEvent> {
        val streamKey = getStreamKey(campaignId)

        return reactiveRedisTemplate.opsForStream<String, Any>()
            .read(
                Consumer.from(groupName, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed())
            )
            .map { record -> mapRecordToEvent(record) }
            .doOnNext { event ->
                // ACK 처리
                reactiveRedisTemplate.opsForStream<String, Any>()
                    .acknowledge(streamKey, groupName, record.id)
            }
    }

    // 🆕 Consumer Group 생성
    suspend fun createConsumerGroupIfNotExists(
        campaignId: Long,
        groupName: String
    ) {
        try {
            val streamKey = getStreamKey(campaignId)
            reactiveRedisTemplate.opsForStream<String, Any>()
                .createGroup(streamKey, groupName)
                .awaitFirstOrNull()
        } catch (e: Exception) {
            // 이미 존재하면 무시
        }
    }
}
```

### Phase 2: 실시간 집계 Worker 구현

```kotlin
@Service
class RealtimeAggregationWorker(
    private val streamService: CampaignDashboardStreamService
) {

    @PostConstruct
    suspend fun start() {
        val instanceId = UUID.randomUUID().toString()

        streamService.createConsumerGroupIfNotExists(
            campaignId = ALL_CAMPAIGNS,
            groupName = "realtime-aggregator"
        )

        streamService.streamEventsWithConsumerGroup(
            campaignId = ALL_CAMPAIGNS,
            groupName = "realtime-aggregator",
            consumerName = "worker-$instanceId"
        ).collect { event ->
            // 각 이벤트를 1번만 집계
            updateRealtimeMetrics(event)
        }
    }
}
```

**효과**:
```
서버 1대: 10,000 events/sec 처리
서버 3대: 30,000 events/sec 처리 (3배 향상!)
```

### Phase 3: 알림 발송 Worker 구현

```kotlin
@Service
class NotificationWorker(
    private val streamService: CampaignDashboardStreamService,
    private val emailService: EmailService
) {

    @PostConstruct
    suspend fun start() {
        val instanceId = InetAddress.getLocalHost().hostName

        streamService.createConsumerGroupIfNotExists(
            campaignId = ALL_CAMPAIGNS,
            groupName = "notification-sender"
        )

        streamService.streamEventsWithConsumerGroup(
            campaignId = ALL_CAMPAIGNS,
            groupName = "notification-sender",
            consumerName = "worker-$instanceId"
        ).collect { event ->
            // 각 알림을 1번만 발송
            emailService.send(
                to = event.userId,
                subject = "New event: ${event.eventName}"
            )
        }
    }
}
```

---

## 🎯 설계 원칙

### 1. 단일 책임 원칙 (SRP)

**현재 구현**:
```kotlin
// ✅ 모니터링 전용 (Broadcast)
fun streamEvents(campaignId: Long): Flux<CampaignDashboardEvent>
```

**향후 추가**:
```kotlin
// ✅ 작업 분산 전용 (Consumer Group)
fun streamEventsWithConsumerGroup(
    campaignId: Long,
    groupName: String,
    consumerName: String
): Flux<CampaignDashboardEvent>
```

→ **2개의 독립적인 메서드로 분리**

### 2. 명시적 의도 (Explicit Intent)

```kotlin
// ❌ 나쁜 예 - 의도가 불명확
fun streamEvents(
    campaignId: Long,
    useConsumerGroup: Boolean = false  // 뭘 위한 플래그?
)

// ✅ 좋은 예 - 의도가 명확
fun streamEvents(campaignId: Long)  // 모니터링용
fun streamEventsWithConsumerGroup(...)  // 작업 분산용
```

### 3. YAGNI (You Aren't Gonna Need It)

**현재**: Consumer Group 사용하지 않음 → 구현하지 않음

**미래**: 필요할 때 추가

---

## 📚 관련 문서

- [README.md](README.md#향후-개선-사항) - Consumer Group 추가 계획
- [IMPLEMENTATION.md](IMPLEMENTATION.md#설계-결정-사항) - 설계 결정 배경
- [Redis Streams Documentation](https://redis.io/docs/data-types/streams/)

---

## 🔑 핵심 요약

| 항목 | 내용 |
|------|------|
| **현재 상태** | Consumer Group 미구현 (제거됨) |
| **제거 이유** | SSE 모니터링 목적 (Broadcast 필요) |
| **현재 동작** | 모든 클라이언트가 모든 이벤트 수신 |
| **향후 계획** | 작업 분산이 필요한 경우 추가 예정 |
| **추가 시점** | 실시간 집계 Worker, 알림 발송 Worker 구현 시 |

**결론**: 지금은 필요 없지만, 나중에 작업 분산이 필요하면 추가할 예정입니다.
