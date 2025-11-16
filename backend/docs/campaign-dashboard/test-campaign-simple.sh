#!/bin/bash

# Campaign Dashboard 간단 테스트 스크립트
# 캠페인 생성 → 이벤트 발행 → 대시보드 조회

BASE_URL="http://localhost:8080"
CAMPAIGN_NAME="simple-test-$(date +%s)"

echo "🚀 캠페인 대시보드 테스트 시작"
echo ""

# 1. 사용자 생성
echo "1. 사용자 생성..."
USER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/users" \
  -H "Content-Type: application/json" \
  -d "{\"externalId\": \"testuser-$CAMPAIGN_NAME\", \"userAttributes\": \"{\\\"email\\\":\\\"test@example.com\\\",\\\"name\\\":\\\"Test User\\\"}\"}")
USER_EXTERNAL_ID=$(echo "$USER_RESPONSE" | jq -r '.data.externalId')
echo "   ✅ 사용자 ID: $USER_EXTERNAL_ID"
echo ""

# 2. 캠페인 생성
echo "2. 캠페인 생성..."
RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/events/campaign" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"$CAMPAIGN_NAME\", \"properties\": []}")

CAMPAIGN_ID=$(echo "$RESPONSE" | jq -r '.data.id')
echo "   ✅ 캠페인 ID: $CAMPAIGN_ID"
echo "   📄 응답: $RESPONSE"
echo ""

# 3. 이벤트 5개 발행
echo "3. 이벤트 발행 (5개)..."
for i in {1..5}; do
  curl -s -X POST "$BASE_URL/api/v1/events" \
    -H "Content-Type: application/json" \
    -d "{
      \"name\": \"click_event_$i\",
      \"campaignName\": \"$CAMPAIGN_NAME\",
      \"externalId\": \"$USER_EXTERNAL_ID\",
      \"properties\": []
    }" > /dev/null
  echo -n "."
  sleep 0.2
done
echo " ✅"
echo ""

# 4. 대시보드 조회
echo "4. 대시보드 요약 조회"
echo ""
curl -s "$BASE_URL/api/v1/campaigns/$CAMPAIGN_ID/dashboard/summary" | jq .
echo ""

echo "=========================================="
echo "📊 대시보드 확인:"
echo "curl '$BASE_URL/api/v1/campaigns/$CAMPAIGN_ID/dashboard' | jq ."
echo ""
echo "🌊 실시간 스트리밍:"
echo "curl -N '$BASE_URL/api/v1/campaigns/$CAMPAIGN_ID/dashboard/stream'"
echo "=========================================="
