#!/bin/bash

# Quick test script for Go event service
# This script tests basic functionality of the Go service

echo "======================================================"
echo "   Go Event Service Quick Test"
echo "======================================================"
echo ""

# Check if service is running
if ! curl -s http://localhost:8081/health > /dev/null; then
    echo "❌ Service is not running on port 8081"
    echo ""
    echo "Please start the service first:"
    echo "  cd event-service-go"
    echo "  go run main.go"
    echo ""
    exit 1
fi

echo "✅ Service is running"
echo ""

# Test 1: Health Check
echo "Test 1: Health Check"
echo "----------------------------"
HEALTH=$(curl -s http://localhost:8081/health)
echo "Response: $HEALTH"
echo ""

# Test 2: Create Event (should fail if user doesn't exist)
echo "Test 2: Create Event"
echo "----------------------------"
EVENT_RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-event",
    "externalId": "test-user-1",
    "properties": [
      {"key": "test", "value": "value"}
    ]
  }')
echo "Response: $EVENT_RESPONSE"
echo ""

# Test 3: Create Campaign
echo "Test 3: Create Campaign"
echo "----------------------------"
CAMPAIGN_RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/events/campaign \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-campaign-'$(date +%s)'",
    "properties": [
      {"key": "test", "value": ""}
    ]
  }')
echo "Response: $CAMPAIGN_RESPONSE"
echo ""

# Test 4: Search Events
echo "Test 4: Search Events"
echo "----------------------------"
SEARCH_RESPONSE=$(curl -s "http://localhost:8081/api/v1/events?eventName=test-event&where=test&value&=&end")
echo "Response: $SEARCH_RESPONSE"
echo ""

echo "======================================================"
echo "   Test Complete"
echo "======================================================"
echo ""
echo "Note: Some tests may fail if required data doesn't exist."
echo "Run ./setup-benchmark.sh to create test data."
echo ""
