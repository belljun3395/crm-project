#!/bin/sh

echo "[INFO] Starting CRM App with Pinpoint Agent..."
echo "PROFILE: $PINPOINT_PROFILE"
echo "AGENT PATH: $PINPOINT_AGENT_PATH"

java \
  -XX:MaxGCPauseMillis=100 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:MaxRAMPercentage=80.0 \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  - ${JAVA_OPTS} \
  -jar \
  -javaagent:${PINPOINT_AGENT_PATH}/pinpoint-bootstrap.jar \
  -Dpinpoint.profiler.profiles.active=release \
  -Dpinpoint.applicationName=crm-${PINPOINT_PROFILE} \
  app.jar
