# Campaign Dashboard API 명세서

## 기본 정보

**Base URL**: `http://localhost:8080/api/v1/campaigns`

**인증**: 현재 미구현 (추후 JWT/OAuth2 적용 예정)

**Content-Type**: `application/json`

---

## API 엔드포인트 목록

| Method | Endpoint | 설명 | UseCase |
|--------|----------|------|---------|
| GET | `/{campaignId}/dashboard` | 대시보드 조회 | GetCampaignDashboardUseCase |
| GET | `/{campaignId}/dashboard/stream` | 실시간 스트리밍 (SSE) | - |
| GET | `/{campaignId}/dashboard/summary` | 요약 정보 조회 | GetCampaignSummaryUseCase |
| GET | `/{campaignId}/dashboard/stream/status` | 스트림 상태 조회 | GetStreamStatusUseCase |

---

## 1. 캠페인 대시보드 조회

### Request
```http
GET /api/v1/campaigns/{campaignId}/dashboard
```

#### Path Parameters
| Name | Type | Required | Description |
|------|------|----------|-------------|
| campaignId | Long | ✅ | 캠페인 ID |

#### Query Parameters
| Name | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| startTime | LocalDateTime | ❌ | null | 조회 시작 시간 (ISO 8601: `YYYY-MM-DDTHH:mm:ss`) |
| endTime | LocalDateTime | ❌ | null | 조회 종료 시간 (ISO 8601) |
| timeWindowUnit | TimeWindowUnit | ❌ | null | 시간 단위 (`MINUTE`, `HOUR`, `DAY`, `WEEK`, `MONTH`) |

#### Query 조합 규칙
1. **모든 파라미터 없음** → 전체 메트릭 조회
2. **timeWindowUnit만** → 해당 단위의 최근 7일 메트릭
3. **startTime + endTime** → 해당 기간의 모든 메트릭
4. **timeWindowUnit + startTime** → 해당 단위의 startTime 이후 메트릭

### Response

#### Success (200 OK)
```json
{
  "success": true,
  "code": 200,
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
        "timeWindowUnit": "HOUR",
        "createdAt": "2025-11-16T13:05:00",
        "updatedAt": "2025-11-16T13:59:30"
      },
      {
        "id": 101,
        "campaignId": 1,
        "metricType": "EVENT_COUNT",
        "metricValue": 980,
        "timeWindowStart": "2025-11-16T14:00:00",
        "timeWindowEnd": "2025-11-16T15:00:00",
        "timeWindowUnit": "HOUR",
        "createdAt": "2025-11-16T14:01:00",
        "updatedAt": "2025-11-16T14:45:00"
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

#### Error Cases
| Status | Code | Message | 원인 |
|--------|------|---------|------|
| 400 | BAD_REQUEST | Invalid time range | startTime > endTime |
| 404 | NOT_FOUND | Campaign not found | 존재하지 않는 campaignId |
| 500 | INTERNAL_SERVER_ERROR | Database error | DB 연결 오류 |

### cURL 예시
```bash
# 1. 전체 메트릭
curl http://localhost:8080/api/v1/campaigns/1/dashboard

# 2. 시간 범위 지정
curl "http://localhost:8080/api/v1/campaigns/1/dashboard?startTime=2025-11-16T00:00:00&endTime=2025-11-16T23:59:59"

# 3. 시간 단위 지정 (시간별)
curl "http://localhost:8080/api/v1/campaigns/1/dashboard?timeWindowUnit=HOUR"

# 4. 조합 (일별, 지난 30일)
curl "http://localhost:8080/api/v1/campaigns/1/dashboard?timeWindowUnit=DAY&startTime=2025-10-16T00:00:00"
```

---

## 2. 실시간 스트리밍 (SSE)

### Request
```http
GET /api/v1/campaigns/{campaignId}/dashboard/stream
```

#### Path Parameters
| Name | Type | Required | Description |
|------|------|----------|-------------|
| campaignId | Long | ✅ | 캠페인 ID |

#### Query Parameters
| Name | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| durationSeconds | Long | ❌ | 3600 | 스트리밍 지속 시간 (초) |

### Response

#### Success (200 OK)
**Content-Type**: `text/event-stream`

#### Event Types

##### 1. campaign-event (실시간 이벤트)
```
event: campaign-event
id: 100
data: {"campaignId":1,"eventId":100,"userId":50,"eventName":"click","timestamp":"2025-11-16T14:30:00"}
```

##### 2. error (에러 발생)
```
event: error
data: Stream error occurred
```

##### 3. stream-end (스트림 종료)
```
event: stream-end
data: Stream ended
```

#### Error Cases
| Status | Description |
|--------|-------------|
| 404 | Campaign not found |
| 500 | Redis connection error |

### 사용 예시

#### cURL
```bash
# 기본 (1시간)
curl -N http://localhost:8080/api/v1/campaigns/1/dashboard/stream

# 10분 동안만
curl -N "http://localhost:8080/api/v1/campaigns/1/dashboard/stream?durationSeconds=600"
```

#### JavaScript (EventSource)
```javascript
const eventSource = new EventSource(
  'http://localhost:8080/api/v1/campaigns/1/dashboard/stream?durationSeconds=60'
);

// 이벤트 수신
eventSource.addEventListener('campaign-event', (event) => {
  const data = JSON.parse(event.data);
  console.log('Event received:', data);

  // UI 업데이트
  updateDashboard(data);
});

// 스트림 종료
eventSource.addEventListener('stream-end', () => {
  console.log('Stream ended');
  eventSource.close();
});

// 에러 처리
eventSource.onerror = (error) => {
  console.error('Connection error:', error);
  eventSource.close();
};
```

#### Python
```python
import sseclient
import requests

url = 'http://localhost:8080/api/v1/campaigns/1/dashboard/stream'
response = requests.get(url, stream=True)
client = sseclient.SSEClient(response)

for event in client.events():
    if event.event == 'campaign-event':
        data = json.loads(event.data)
        print(f"Event: {data['eventName']}, User: {data['userId']}")
    elif event.event == 'stream-end':
        break
```

---

## 3. 캠페인 요약 정보 조회

### Request
```http
GET /api/v1/campaigns/{campaignId}/dashboard/summary
```

#### Path Parameters
| Name | Type | Required | Description |
|------|------|----------|-------------|
| campaignId | Long | ✅ | 캠페인 ID |

### Response

#### Success (200 OK)
```json
{
  "success": true,
  "code": 200,
  "data": {
    "campaignId": 1,
    "totalEvents": 5000,
    "eventsLast24Hours": 1200,
    "eventsLast7Days": 3500,
    "lastUpdated": "2025-11-16T14:30:00"
  }
}
```

#### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| campaignId | Long | 캠페인 ID |
| totalEvents | Long | 전체 이벤트 수 |
| eventsLast24Hours | Long | 최근 24시간 이벤트 수 |
| eventsLast7Days | Long | 최근 7일 이벤트 수 |
| lastUpdated | LocalDateTime | 마지막 업데이트 시각 |

#### Error Cases
| Status | Message |
|--------|---------|
| 404 | Campaign not found |

### cURL 예시
```bash
curl http://localhost:8080/api/v1/campaigns/1/dashboard/summary
```

---

## 4. 스트림 상태 조회

### Request
```http
GET /api/v1/campaigns/{campaignId}/dashboard/stream/status
```

#### Path Parameters
| Name | Type | Required | Description |
|------|------|----------|-------------|
| campaignId | Long | ✅ | 캠페인 ID |

### Response

#### Success (200 OK)
```json
{
  "success": true,
  "code": 200,
  "data": {
    "campaignId": 1,
    "streamLength": 350,
    "checkedAt": "2025-11-16T14:30:00"
  }
}
```

#### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| campaignId | Long | 캠페인 ID |
| streamLength | Long | 현재 Stream에 저장된 이벤트 수 (최대 10,000) |
| checkedAt | LocalDateTime | 조회 시각 |

#### Error Cases
| Status | Message |
|--------|---------|
| 404 | Campaign not found |
| 500 | Redis connection error |

### cURL 예시
```bash
# 현재 상태 확인
curl http://localhost:8080/api/v1/campaigns/1/dashboard/stream/status

# 모니터링 (5초마다 확인)
watch -n 5 'curl -s http://localhost:8080/api/v1/campaigns/1/dashboard/stream/status | jq .data.streamLength'
```

---

## 데이터 타입 정의

### TimeWindowUnit (Enum)
```
MINUTE  - 1분 단위
HOUR    - 1시간 단위
DAY     - 1일 단위
WEEK    - 1주 단위 (월요일 시작)
MONTH   - 1개월 단위
```

### MetricType (Enum)
```
EVENT_COUNT        - 이벤트 발생 횟수 (구현됨)
UNIQUE_USER_COUNT  - 고유 사용자 수 (미구현)
TOTAL_USER_COUNT   - 전체 사용자 수 (미구현)
```

**metricValue와의 관계**:
- `EVENT_COUNT`일 때: metricValue = 해당 시간 윈도우의 이벤트 개수
  - 예: metricValue = 1250 → 1,250개의 이벤트 발생
- `UNIQUE_USER_COUNT`일 때: metricValue = 고유 사용자 수
  - 예: metricValue = 850 → 850명의 다른 사용자
- `TOTAL_USER_COUNT`일 때: metricValue = 전체 사용자 수
  - 예: metricValue = 1250 → 동일 사용자 중복 포함

### LocalDateTime 형식
```
ISO 8601: YYYY-MM-DDTHH:mm:ss
예시: 2025-11-16T14:30:00
```

---

## Rate Limiting

현재 미구현

향후 계획:
- IP 기반: 100 req/min
- User 기반: 1000 req/min
- SSE 연결: 동시 10개/user

---

## Changelog

### v1.0.0 (2025-11-16)
- ✅ 초기 릴리스
- ✅ 4개 엔드포인트 구현
- ✅ UseCase 패턴 적용
- ✅ SSE 실시간 스트리밍

---

## Support

문의: [GitHub Issues](https://github.com/your-repo/issues)
