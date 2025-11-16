#!/bin/bash

# 색상 코드
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# --- 벤치마킹 설정 ---
KOTLIN_SPRING_URL="http://localhost:8080/api/v1/events"
GO_URL="http://localhost:8081/api/v1/events"
K6_SCRIPT_PATH="benchmark/test.js"

echo -e "${YELLOW}=== CRM Event Service Performance Benchmark ===${NC}"
echo ""
echo "This benchmark compares:"
echo "  1. Kotlin + Spring Boot (reactive) on port 8080"
echo "  2. Go service on port 8081"
echo ""
echo -e "${YELLOW}Test configuration:${NC}"
echo "  - Virtual Users: 100"
echo "  - Duration: 30 seconds"
echo "  - Endpoint: POST /api/v1/events"
echo ""

# --- Kotlin + Spring Boot 벤치마킹 ---
echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}   1. Kotlin + Spring Boot Benchmark${NC}"
echo -e "${BLUE}=====================================================${NC}"
cd backend
./gradlew bootRun > /dev/null 2>&1 &
SPRING_PID=$!
echo "Spring Boot server started with PID: $SPRING_PID"
echo "Waiting 25 seconds for initialization..."
sleep 25

echo -e "${GREEN}Running k6 load test on Spring Boot...${NC}"
k6 run --env TARGET_URL=$KOTLIN_SPRING_URL ../$K6_SCRIPT_PATH

echo ""
echo "Stopping Spring Boot server..."
kill $SPRING_PID
wait $SPRING_PID 2>/dev/null
echo -e "${BLUE}Kotlin + Spring Boot benchmark finished${NC}"
cd ..

echo ""
echo -e "${YELLOW}-----------------------------------------------------${NC}"
echo ""
sleep 3

# --- Go 벤치마킹 ---
echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}   2. Go Service Benchmark${NC}"
echo -e "${BLUE}=====================================================${NC}"
cd event-service-go
/opt/homebrew/bin/go run main.go > /dev/null 2>&1 &
GO_PID=$!
echo "Go server started with PID: $GO_PID"
echo "Waiting 3 seconds for initialization..."
sleep 3

echo -e "${GREEN}Running k6 load test on Go service...${NC}"
k6 run --env TARGET_URL=$GO_URL ../$K6_SCRIPT_PATH

echo ""
echo "Stopping Go server..."
kill $GO_PID
wait $GO_PID 2>/dev/null
echo -e "${BLUE}Go service benchmark finished${NC}"
cd ..

echo ""
echo -e "${YELLOW}=====================================================${NC}"
echo -e "${GREEN}All benchmarks are complete!${NC}"
echo -e "${YELLOW}=====================================================${NC}"
echo ""
echo "Compare the results above to see:"
echo "  - Response times (avg, min, max, p95, p99)"
echo "  - Requests per second"
echo "  - Failed requests"
echo "  - Data transfer rates"
echo ""
