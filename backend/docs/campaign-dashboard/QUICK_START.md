# Campaign Dashboard - 빠른 시작 가이드

## 🚀 5분 안에 시작하기

### 1단계: 캠페인 생성
```bash
curl -X POST http://localhost:8080/api/v1/events/campaign \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-campaign",
    "properties": [
      {"key": "type", "value": "email"},
      {"key": "target", "value": "new-users"}
    ]
  }'
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "message": "Campaign created successfully"
  }
}
```

### 2단계: 이벤트 발행
```bash
# 여러 번 실행해서 테스트 데이터 생성
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "email_opened",
    "campaignName": "test-campaign",
    "externalId": "user123",
    "properties": [
      {"key": "type", "value": "email"},
      {"key": "target", "value": "new-users"}
    ]
  }'
```

### 3단계: 대시보드 확인
```bash
# 요약 정보 조회
curl http://localhost:8080/api/v1/campaigns/1/dashboard/summary
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "campaignId": 1,
    "totalEvents": 150,          // 전체 이벤트 개수
    "eventsLast24Hours": 150,    // 최근 24시간 이벤트 개수
    "eventsLast7Days": 150,      // 최근 7일 이벤트 개수
    "lastUpdated": "2025-11-16T14:30:00"
  }
}
```

> 💡 **참고**: 이 숫자들은 모두 `metricValue`(EVENT_COUNT 타입)의 합계입니다.

### 4단계: 실시간 스트리밍 체험

**터미널 1 - 스트리밍 시작:**
```bash
curl -N http://localhost:8080/api/v1/campaigns/1/dashboard/stream
```

**터미널 2 - 이벤트 발행:**
```bash
# 반복 실행
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/v1/events \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"user_click_$i\",
      \"campaignName\": \"test-campaign\",
      \"externalId\": \"user$i\",
      \"properties\": [{\"key\": \"type\", \"value\": \"email\"}]
    }"
  sleep 1
done
```

터미널 1에서 실시간으로 이벤트가 표시됩니다! 🎉

---

## 📊 주요 사용 패턴

### Pattern 1: 시간대별 분석
```bash
# 오늘 시간별 이벤트 수
curl "http://localhost:8080/api/v1/campaigns/1/dashboard?timeWindowUnit=HOUR&startTime=2025-11-16T00:00:00"
```

### Pattern 2: 특정 기간 분석
```bash
# 지난 7일 데이터
START=$(date -u -v-7d +"%Y-%m-%dT%H:%M:%S")
END=$(date -u +"%Y-%m-%dT%H:%M:%S")

curl "http://localhost:8080/api/v1/campaigns/1/dashboard?startTime=$START&endTime=$END"
```

### Pattern 3: 모니터링
```bash
# Stream 상태 확인
curl http://localhost:8080/api/v1/campaigns/1/dashboard/stream/status

# 예상 출력:
# {
#   "success": true,
#   "data": {
#     "campaignId": 1,
#     "streamLength": 350,  # 현재 Stream에 저장된 이벤트 수
#     "checkedAt": "2025-11-16T14:30:00"
#   }
# }
```

---

## 🔍 문제 해결

### Q: SSE 스트림이 바로 끊어져요
**A:** `-N` 옵션을 사용하세요
```bash
curl -N http://localhost:8080/api/v1/campaigns/1/dashboard/stream
```

### Q: 이벤트가 스트림에 안 나타나요
**A:** 다음을 확인하세요:
1. 이벤트 발행 시 올바른 `campaignName` 사용
2. Campaign이 존재하는지 확인
3. Event properties가 Campaign properties와 일치하는지 확인

### Q: Stream이 계속 커져요
**A:** 자동으로 관리됩니다:
- 100개 이벤트마다 자동 trim 실행
- 최대 10,000개만 유지
- 수동 확인: `/dashboard/stream/status` 엔드포인트 사용

### Q: 과거 메트릭을 보고 싶어요
**A:** 시간 범위를 지정하세요:
```bash
curl "http://localhost:8080/api/v1/campaigns/1/dashboard?startTime=2025-11-01T00:00:00&endTime=2025-11-15T23:59:59"
```

---

## 📚 다음 단계

- 📖 [전체 문서 읽기](README.md)
- 🔌 [API 상세 명세](API.md)
- 🏗️ [아키텍처 이해하기](README.md#아키텍처)

---

## 💡 팁

### JavaScript로 SSE 구독하기
```javascript
const eventSource = new EventSource('http://localhost:8080/api/v1/campaigns/1/dashboard/stream');

eventSource.addEventListener('campaign-event', (event) => {
  const data = JSON.parse(event.data);
  console.log('New event:', data);
});

eventSource.addEventListener('stream-end', () => {
  console.log('Stream ended');
  eventSource.close();
});

eventSource.onerror = (error) => {
  console.error('Stream error:', error);
};
```

### 데이터 시각화 예시
```bash
# 시간별 이벤트 수를 CSV로 추출
curl "http://localhost:8080/api/v1/campaigns/1/dashboard?timeWindowUnit=HOUR" \
  | jq -r '.data.metrics[] | [.timeWindowStart, .metricValue] | @csv'
```
