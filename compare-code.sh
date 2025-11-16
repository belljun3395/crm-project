#!/bin/bash

# Comparison script for codebase metrics

echo "======================================================"
echo "   Code Comparison: Kotlin/Spring vs Go"
echo "======================================================"
echo ""

# Kotlin/Spring Event Module
echo "Kotlin/Spring Event Module:"
echo "----------------------------"
KOTLIN_FILES=$(find backend/src/main/kotlin/com/manage/crm/event -name "*.kt" | wc -l)
KOTLIN_LINES=$(find backend/src/main/kotlin/com/manage/crm/event -name "*.kt" -exec wc -l {} + | tail -1 | awk '{print $1}')
echo "  Files: $KOTLIN_FILES"
echo "  Lines of code: $KOTLIN_LINES"
echo ""

# Go Event Service
echo "Go Event Service:"
echo "----------------------------"
GO_FILES=$(find event-service-go -name "*.go" | wc -l)
GO_LINES=$(find event-service-go -name "*.go" -exec wc -l {} + | tail -1 | awk '{print $1}')
echo "  Files: $GO_FILES"
echo "  Lines of code: $GO_LINES"
echo ""

# Binary sizes (if built)
echo "Binary Sizes:"
echo "----------------------------"
if [ -f "event-service-go/event-service" ]; then
    GO_SIZE=$(ls -lh event-service-go/event-service | awk '{print $5}')
    echo "  Go binary: $GO_SIZE"
else
    echo "  Go binary: Not built yet (run: cd event-service-go && go build)"
fi

SPRING_JAR=$(find backend/build/libs -name "*.jar" 2>/dev/null | head -1)
if [ -n "$SPRING_JAR" ]; then
    SPRING_SIZE=$(ls -lh "$SPRING_JAR" | awk '{print $5}')
    echo "  Spring Boot JAR: $SPRING_SIZE"
else
    echo "  Spring Boot JAR: Not built yet (run: cd backend && ./gradlew build)"
fi
echo ""

echo "Complexity Comparison:"
echo "----------------------------"
echo "  Kotlin/Spring Dependencies: ~50+ (Spring Boot, R2DBC, Kotlin coroutines, etc.)"
echo "  Go Dependencies: 3 (gin, mysql driver, redis)"
echo ""

echo "Startup Time (approximate):"
echo "----------------------------"
echo "  Kotlin/Spring Boot: 20-30 seconds"
echo "  Go: <1 second"
echo ""

echo "Memory Usage (approximate):"
echo "----------------------------"
echo "  Kotlin/Spring Boot: 400-600 MB"
echo "  Go: 50-150 MB"
echo ""
