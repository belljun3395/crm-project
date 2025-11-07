#!/bin/bash

# Campaign Dashboard 전체 플로우 테스트 스크립트
# 캠페인 생성 → 이벤트 발행 → 대시보드 조회

set -e  # 에러 발생 시 스크립트 중단

BASE_URL="http://localhost:8080"
CAMPAIGN_NAME="test-campaign-$(date +%s)"

echo "=========================================="
echo "🚀 Campaign Dashboard 통합 테스트 시작"
echo "=========================================="
echo ""

# ========================================
# 1. 사용자 생성
# ========================================
echo "👤 1단계: 사용자 생성"
echo "------------------------------------------"

CREATE_USER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/users" \
  -H "Content-Type: application/json" \
  -d "{
    \"externalId\": \"testuser-$(date +%s)\",
    \"userAttributes\": \"{\\\"email\\\":\\\"test@example.com\\\",\\\"name\\\":\\\"Test User\\\"}\"
  }")

echo "✅ 사용자 생성 완료"
echo "$CREATE_USER_RESPONSE" | jq .
echo ""

USER_EXTERNAL_ID=$(echo "$CREATE_USER_RESPONSE" | jq -r '.data.externalId')
echo "📌 생성된 사용자 External ID: $USER_EXTERNAL_ID"
echo ""
sleep 1

# ========================================
# 2. 캠페인 생성
# ========================================
echo "📝 2단계: 캠페인 생성"
echo "캠페인명: $CAMPAIGN_NAME"
echo "------------------------------------------"

CREATE_CAMPAIGN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/events/campaign" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"$CAMPAIGN_NAME\",
    \"properties\": []
  }")

echo "✅ 캠페인 생성 완료"
echo "$CREATE_CAMPAIGN_RESPONSE" | jq .
echo ""

# campaignId 추출
CAMPAIGN_ID=$(echo "$CREATE_CAMPAIGN_RESPONSE" | jq -r '.data.id')

if [ "$CAMPAIGN_ID" == "null" ] || [ -z "$CAMPAIGN_ID" ]; then
  echo "❌ 캠페인 ID를 찾을 수 없습니다."
  exit 1
fi

echo "📌 생성된 캠페인 ID: $CAMPAIGN_ID"
echo ""
sleep 1

# ========================================
# 3. 이벤트 발행 (10개)
# ========================================
echo "📤 3단계: 이벤트 발행 (10개)"
echo "------------------------------------------"

for i in {1..10}; do
  EVENT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/events" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"user_action_$i\",
      \"campaignName\": \"$CAMPAIGN_NAME\",
      \"externalId\": \"$USER_EXTERNAL_ID\",
      \"properties\": []
    }")

  EVENT_ID=$(echo "$EVENT_RESPONSE" | jq -r '.data.id')
  echo "  ✓ 이벤트 $i 발행 완료 (ID: $EVENT_ID)"
  sleep 0.3
done

echo ""
echo "✅ 총 10개 이벤트 발행 완료"
echo ""
sleep 1

# ========================================
# 4. 대시보드 요약 정보 조회
# ========================================
echo "📊 4단계: 대시보드 요약 정보 조회"
echo "------------------------------------------"

SUMMARY_RESPONSE=$(curl -s "$BASE_URL/api/v1/campaigns/$CAMPAIGN_ID/dashboard/summary")
echo "$SUMMARY_RESPONSE" | jq .

TOTAL_EVENTS=$(echo "$SUMMARY_RESPONSE" | jq -r '.data.totalEvents')
echo ""
echo "📈 총 이벤트 수: $TOTAL_EVENTS"
echo ""
sleep 1

# ========================================
# 5. 대시보드 메트릭 조회 (HOUR 단위)
# ========================================
echo "📊 5단계: 대시보드 메트릭 조회 (HOUR 단위)"
echo "------------------------------------------"

DASHBOARD_RESPONSE=$(curl -s "$BASE_URL/api/v1/campaigns/$CAMPAIGN_ID/dashboard?timeWindowUnit=HOUR")
echo "$DASHBOARD_RESPONSE" | jq .
echo ""
sleep 1

# ========================================
# 6. Redis Stream 상태 조회
# ========================================
echo "🔍 6단계: Redis Stream 상태 조회"
echo "------------------------------------------"

STREAM_STATUS_RESPONSE=$(curl -s "$BASE_URL/api/v1/campaigns/$CAMPAIGN_ID/dashboard/stream/status")
echo "$STREAM_STATUS_RESPONSE" | jq .

STREAM_LENGTH=$(echo "$STREAM_STATUS_RESPONSE" | jq -r '.data.streamLength')
echo ""
echo "📊 Stream 길이: $STREAM_LENGTH"
echo ""

# ========================================
# 7. 실시간 스트리밍 테스트 (5초)
# ========================================
echo "🌊 7단계: 실시간 스트리밍 테스트 (5초간)"
echo "------------------------------------------"
echo "스트리밍 시작... (백그라운드에서 5초간 실행)"
echo ""

# 백그라운드에서 스트리밍 시작
timeout 5s curl -N -s "$BASE_URL/api/v1/campaigns/$CAMPAIGN_ID/dashboard/stream?durationSeconds=5" &
STREAM_PID=$!

# 스트리밍 중 추가 이벤트 발행 (3개)
sleep 1
for i in {11..13}; do
  curl -s -X POST "$BASE_URL/api/v1/events" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"streaming_test_$i\",
      \"campaignName\": \"$CAMPAIGN_NAME\",
      \"externalId\": \"$USER_EXTERNAL_ID\",
      \"properties\": []
    }" > /dev/null
  echo "  ✓ 스트리밍 중 이벤트 $i 발행"
  sleep 1
done

# 스트리밍 종료 대기
wait $STREAM_PID 2>/dev/null || true
echo ""
echo "✅ 스트리밍 테스트 완료"
echo ""

# ========================================
# 8. 최종 결과 확인
# ========================================
echo "=========================================="
echo "📋 8단계: 최종 결과 요약"
echo "=========================================="

FINAL_SUMMARY=$(curl -s "$BASE_URL/api/v1/campaigns/$CAMPAIGN_ID/dashboard/summary")
FINAL_TOTAL=$(echo "$FINAL_SUMMARY" | jq -r '.data.totalEvents')
FINAL_24H=$(echo "$FINAL_SUMMARY" | jq -r '.data.eventsLast24Hours')

echo "캠페인 ID: $CAMPAIGN_ID"
echo "캠페인명: $CAMPAIGN_NAME"
echo "총 이벤트: $FINAL_TOTAL 개"
echo "최근 24시간: $FINAL_24H 개"
echo ""

# ========================================
# 9. 대시보드 URL 출력
# ========================================
echo "=========================================="
echo "🔗 9단계: 대시보드 접속 정보"
echo "=========================================="
echo ""
echo "📊 대시보드 조회:"
echo "  curl '$BASE_URL/api/v1/campaigns/$CAMPAIGN_ID/dashboard'"
echo ""
echo "📈 요약 정보:"
echo "  curl '$BASE_URL/api/v1/campaigns/$CAMPAIGN_ID/dashboard/summary'"
echo ""
echo "🌊 실시간 스트리밍:"
echo "  curl -N '$BASE_URL/api/v1/campaigns/$CAMPAIGN_ID/dashboard/stream'"
echo ""
echo "🔍 Stream 상태:"
echo "  curl '$BASE_URL/api/v1/campaigns/$CAMPAIGN_ID/dashboard/stream/status'"
echo ""

echo "=========================================="
echo "✅ 전체 테스트 완료!"
echo "=========================================="
