# Campaign Dashboard - 용어 사전

## 핵심 개념

### metricValue
**정의**: 특정 시간 윈도우 동안 측정된 메트릭의 수치

**의미**: `metricType`에 따라 달라짐

| MetricType | metricValue 의미 | 예시 값 | 설명 | 구현 |
|------------|------------------|---------|------|------|
| **EVENT_COUNT** | 이벤트 발생 횟수 | 1,250 | 해당 시간대에 1,250번의 이벤트 발생 | ✅ |
| **UNIQUE_USER_COUNT** | 고유 사용자 수 | 850 | 850명의 서로 다른 사용자 | ❌ |
| **TOTAL_USER_COUNT** | 전체 사용자 수 | 1,250 | 중복 포함 전체 사용자 (동일 사용자 여러 번 카운트) | ❌ |

**실제 데이터 예시**:
```json
{
  "metricType": "EVENT_COUNT",
  "metricValue": 1250,                 // ← 이 값이 중요!
  "timeWindowStart": "2025-11-16T13:00:00",
  "timeWindowEnd": "2025-11-16T14:00:00",
  "timeWindowUnit": "HOUR"
}
```
**해석**: "2025년 11월 16일 13시~14시 사이에 1,250개의 이벤트가 발생했다"

---

### timeWindowUnit
**정의**: 메트릭을 집계하는 시간 단위

| 값 | 의미 | 예시 |
|---|------|------|
| `MINUTE` | 1분 단위 | 13:00:00 ~ 13:01:00 |
| `HOUR` | 1시간 단위 | 13:00:00 ~ 14:00:00 |
| `DAY` | 1일 단위 | 2025-11-16 00:00:00 ~ 23:59:59 |
| `WEEK` | 1주 단위 (월요일 시작) | 2025-11-11 ~ 2025-11-17 |
| `MONTH` | 1개월 단위 | 2025-11-01 ~ 2025-11-30 |

**사용 예시**:
```bash
# 시간별 이벤트 수 조회
curl "http://localhost:8080/api/v1/campaigns/1/dashboard?timeWindowUnit=HOUR"
```

---

### timeWindowStart / timeWindowEnd
**정의**: 메트릭이 집계된 시간 범위

**규칙**:
- `timeWindowStart`: 포함 (inclusive)
- `timeWindowEnd`: 미포함 (exclusive)
- 즉, `[start, end)` 구간

**예시**:
```json
{
  "timeWindowStart": "2025-11-16T13:00:00",  // 13시 00분 00초 포함
  "timeWindowEnd": "2025-11-16T14:00:00",    // 14시 00분 00초 미포함 (13시 59분 59초까지)
  "timeWindowUnit": "HOUR"
}
```

---

### MetricType
**정의**: 측정하려는 메트릭의 종류

**현재 구현**:
- ✅ `EVENT_COUNT`: 이벤트 개수

**향후 확장 예정**:
- ❌ `UNIQUE_USER_COUNT`: 고유 사용자 수 (중복 제거)
- ❌ `TOTAL_USER_COUNT`: 전체 사용자 수 (중복 포함)
- ❌ `CLICK_RATE`: 클릭률
- ❌ `CONVERSION_RATE`: 전환율

---

## 실제 사용 예시

### 시나리오: 시간대별 이벤트 분석

**질문**: "오늘 오후 1시~5시에 몇 개의 이벤트가 발생했나?"

**API 호출**:
```bash
curl "http://localhost:8080/api/v1/campaigns/1/dashboard?timeWindowUnit=HOUR&startTime=2025-11-16T13:00:00"
```

**응답**:
```json
{
  "success": true,
  "data": {
    "metrics": [
      {
        "metricType": "EVENT_COUNT",
        "metricValue": 1250,              // 13시~14시: 1,250개
        "timeWindowStart": "2025-11-16T13:00:00",
        "timeWindowEnd": "2025-11-16T14:00:00"
      },
      {
        "metricType": "EVENT_COUNT",
        "metricValue": 980,               // 14시~15시: 980개
        "timeWindowStart": "2025-11-16T14:00:00",
        "timeWindowEnd": "2025-11-16T15:00:00"
      },
      {
        "metricType": "EVENT_COUNT",
        "metricValue": 1100,              // 15시~16시: 1,100개
        "timeWindowStart": "2025-11-16T15:00:00",
        "timeWindowEnd": "2025-11-16T16:00:00"
      },
      {
        "metricType": "EVENT_COUNT",
        "metricValue": 850,               // 16시~17시: 850개
        "timeWindowStart": "2025-11-16T16:00:00",
        "timeWindowEnd": "2025-11-16T17:00:00"
      }
    ]
  }
}
```

**답**: 총 4,180개 (1,250 + 980 + 1,100 + 850)

---

### 시나리오: 요약 정보

**질문**: "이 캠페인의 전체 이벤트 수와 최근 활동은?"

**API 호출**:
```bash
curl http://localhost:8080/api/v1/campaigns/1/dashboard/summary
```

**응답**:
```json
{
  "success": true,
  "data": {
    "campaignId": 1,
    "totalEvents": 5000,           // 전체 이벤트 수 (모든 metricValue 합)
    "eventsLast24Hours": 1200,     // 최근 24시간 metricValue 합
    "eventsLast7Days": 3500,       // 최근 7일 metricValue 합
    "lastUpdated": "2025-11-16T14:30:00"
  }
}
```

**해석**:
- 캠페인 시작 후 총 5,000개 이벤트
- 어제(24시간) 동안 1,200개
- 지난 주(7일) 동안 3,500개

---

## 집계 방식

### 이벤트 발생 → metricValue 증가

```kotlin
// 이벤트 1개 발생
POST /api/v1/events { name: "click", campaignName: "test" }

// 내부 처리
publishCampaignEvent(event) {
    // 1. Redis Stream 발행 (실시간)
    streamService.publishEvent(event)

    // 2. DB 메트릭 업데이트
    updateMetricsForEvent(event) {
        // HOUR 단위
        metric = findOrCreate(HOUR, 13:00-14:00)
        metric.metricValue += 1    // 1 증가!
        save(metric)

        // DAY 단위
        metric = findOrCreate(DAY, 2025-11-16)
        metric.metricValue += 1    // 1 증가!
        save(metric)
    }
}
```

**결과**: 하나의 이벤트가 여러 시간 단위의 metricValue를 증가시킴

---

## FAQ

### Q: metricValue가 증가하는 타이밍은?
**A**: 이벤트 발행 즉시 (실시간)

### Q: 과거 데이터를 수정할 수 있나?
**A**: 현재는 불가능. 향후 재집계 배치 추가 예정

### Q: metricValue가 음수가 될 수 있나?
**A**: 불가능. `incrementValue()`만 사용하므로 항상 증가

### Q: 동일한 시간 윈도우에 여러 metricType이 있을 수 있나?
**A**: 가능. 하지만 현재는 EVENT_COUNT만 사용

### Q: metricValue의 최대값은?
**A**: Long 타입 (최대 9,223,372,036,854,775,807)

---

### Consumer Group
**정의**: Redis Stream에서 여러 소비자가 협력해서 메시지를 분산 처리하는 기능

**현재 상태**: ❌ 미구현 (제거됨)

**제거 이유**:
- 현재는 실시간 모니터링(SSE)이 목적
- 모든 클라이언트가 같은 이벤트를 봐야 함
- Consumer Group은 작업 분산용 (각 이벤트를 1번만 처리)

**비교**:
```
Without Consumer Group (현재):
- 모든 클라이언트가 모든 이벤트 수신
- 대시보드 모니터링에 적합 ✅

With Consumer Group (미래):
- 각 이벤트를 1개 Worker만 처리
- 이메일 발송, 알림 전송에 적합 ✅
```

**상세 설명**: [CONSUMER_GROUP.md](CONSUMER_GROUP.md)

---

## 관련 문서

- [README.md](README.md) - 전체 개요
- [API.md](API.md) - API 명세
- [QUICK_START.md](QUICK_START.md) - 빠른 시작
- [IMPLEMENTATION.md](IMPLEMENTATION.md) - 구현 상세
- [CONSUMER_GROUP.md](CONSUMER_GROUP.md) - Consumer Group 개념 및 설계 결정
