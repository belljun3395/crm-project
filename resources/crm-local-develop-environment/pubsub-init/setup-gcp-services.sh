#!/bin/sh

# GCP Pub/Sub Emulator Setup Script for Local Development
# Used when CLOUD_PROVIDER=gcp

EMULATOR_HOST="http://crm-pubsub-emulator:8085"
PROJECT_ID="local-project"

echo "Setting up GCP Pub/Sub emulator resources for local development..."

# Create cache invalidation topic
echo "Creating Pub/Sub topic: cache-invalidation-topic"
curl -s -X PUT "${EMULATOR_HOST}/v1/projects/${PROJECT_ID}/topics/cache-invalidation-topic" \
    -H "Content-Type: application/json" \
    && echo "  Topic created: cache-invalidation-topic" \
    || echo "  Topic already exists or failed: cache-invalidation-topic"

# Create cache invalidation subscription
echo "Creating Pub/Sub subscription: cache-invalidation-subscription"
curl -s -X PUT "${EMULATOR_HOST}/v1/projects/${PROJECT_ID}/subscriptions/cache-invalidation-subscription" \
    -H "Content-Type: application/json" \
    -d "{\"topic\": \"projects/${PROJECT_ID}/topics/cache-invalidation-topic\", \"ackDeadlineSeconds\": 30}" \
    && echo "  Subscription created: cache-invalidation-subscription" \
    || echo "  Subscription already exists or failed: cache-invalidation-subscription"

echo ""
echo "GCP Pub/Sub emulator setup completed!"
echo ""
echo "Summary of created resources:"
echo "  Topic:        projects/${PROJECT_ID}/topics/cache-invalidation-topic"
echo "  Subscription: projects/${PROJECT_ID}/subscriptions/cache-invalidation-subscription"
echo ""
echo "Emulator endpoint: ${EMULATOR_HOST}"
echo "To use in application, set:"
echo "  spring.cloud.gcp.pubsub.emulator-host=localhost:8085"
echo "  spring.cloud.gcp.project-id=${PROJECT_ID}"
