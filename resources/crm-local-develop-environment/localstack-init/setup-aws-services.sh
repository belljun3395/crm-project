#!/bin/bash

set -euo pipefail

# LocalStack AWS Services Setup Script for Local Development

LOCALSTACK_HOST="${LOCALSTACK_HOST:-localhost}"
export AWS_ENDPOINT_URL="${AWS_ENDPOINT_URL:-http://${LOCALSTACK_HOST}:4566}"

echo "🚀 Setting up AWS services for local development environment..."

escape_aws_cli_map_value() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

set_queue_policy() {
  local queue_url="$1"
  local policy_json="$2"
  local escaped_policy
  escaped_policy="$(escape_aws_cli_map_value "$policy_json")"

  aws sqs set-queue-attributes \
    --queue-url "$queue_url" \
    --attributes "{\"Policy\":\"${escaped_policy}\"}"
}

# Wait for LocalStack to be ready
echo "⏳ Waiting for LocalStack to be ready..."
while ! curl -s "http://${LOCALSTACK_HOST}:4566/_localstack/health" > /dev/null; do
  echo "   Still waiting for LocalStack..."
  sleep 3
done

echo "✅ LocalStack is ready. Setting up services..."

# Create SES identity for local development
echo "📧 Setting up SES (Simple Email Service)..."

# Use environment variable for email list, default to standard addresses
VERIFIED_EMAILS="${VERIFIED_EMAILS:-test@example.com notification@example.com noreply@local.dev admin@local.dev}"

# Verify email identities (idempotent - skip if already verified)
for EMAIL in ${VERIFIED_EMAILS//,/ }; do
  if [ -z "$EMAIL" ]; then
    continue
  fi
  if ! aws ses list-verified-email-addresses --query "VerifiedEmailAddresses[]" --output text 2>/dev/null | grep -Fxq "$EMAIL"; then
    aws ses verify-email-identity --email-address "$EMAIL" && echo "   ✓ Verified: $EMAIL" || echo "   ⚠ Failed to verify: $EMAIL"
  else
    echo "   ✓ Already verified: $EMAIL"
  fi
done

# Create SES configuration set (idempotent - check if exists first)
if ! aws ses list-configuration-sets --query "ConfigurationSets[?Name=='local-configuration-set']" --output text 2>/dev/null | grep -Fxq "local-configuration-set"; then
  aws ses create-configuration-set --configuration-set Name=local-configuration-set && echo "   ✓ Configuration set created: local-configuration-set" || echo "   ⚠ Failed to create configuration set"
else
  echo "   ✓ Configuration set already exists: local-configuration-set"
fi

echo "✅ SES setup completed"

# Create SQS queue for SES events and general messaging
echo "📨 Setting up SQS (Simple Queue Service)..."
aws sqs create-queue --queue-name ses_sqs
aws sqs create-queue --queue-name notification_queue
aws sqs create-queue --queue-name event_processing_queue

# Create SQS queue for cache invalidation (AWS DR strategy)
# Note: GCP environments use GCP Pub/Sub instead - see pubsub-init/setup-gcp-services.sh
aws sqs create-queue --queue-name crm-dr-cache-invalidation-queue-aws

# Get SQS queue URLs and ARNs
SES_SQS_URL=$(aws sqs get-queue-url --queue-name ses_sqs --query 'QueueUrl' --output text)
SES_SQS_ARN=$(aws sqs get-queue-attributes --queue-url $SES_SQS_URL --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

NOTIFICATION_SQS_URL=$(aws sqs get-queue-url --queue-name notification_queue --query 'QueueUrl' --output text)
NOTIFICATION_SQS_ARN=$(aws sqs get-queue-attributes --queue-url $NOTIFICATION_SQS_URL --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

EVENT_SQS_URL=$(aws sqs get-queue-url --queue-name event_processing_queue --query 'QueueUrl' --output text)
EVENT_SQS_ARN=$(aws sqs get-queue-attributes --queue-url $EVENT_SQS_URL --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

CACHE_AWS_SQS_URL=$(aws sqs get-queue-url --queue-name crm-dr-cache-invalidation-queue-aws --query 'QueueUrl' --output text)
CACHE_AWS_SQS_ARN=$(aws sqs get-queue-attributes --queue-url $CACHE_AWS_SQS_URL --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

echo "✅ SQS queues created"
echo "   📋 SES SQS ARN: $SES_SQS_ARN"
echo "   📋 Notification SQS ARN: $NOTIFICATION_SQS_ARN"
echo "   📋 Event Processing SQS ARN: $EVENT_SQS_ARN"
echo "   📋 Cache Invalidation AWS SQS ARN: $CACHE_AWS_SQS_ARN"

# Create SNS topics for different types of notifications
echo "📢 Setting up SNS (Simple Notification Service)..."
aws sns create-topic --name ses-events-topic
aws sns create-topic --name user-events-topic
aws sns create-topic --name campaign-events-topic
aws sns create-topic --name cache-invalidation-topic

# Get SNS topic ARNs
SES_SNS_ARN=$(aws sns list-topics --query 'Topics[?contains(TopicArn,`ses-events-topic`)].TopicArn' --output text)
USER_SNS_ARN=$(aws sns list-topics --query 'Topics[?contains(TopicArn,`user-events-topic`)].TopicArn' --output text)
CAMPAIGN_SNS_ARN=$(aws sns list-topics --query 'Topics[?contains(TopicArn,`campaign-events-topic`)].TopicArn' --output text)
CACHE_SNS_ARN=$(aws sns list-topics --query 'Topics[?contains(TopicArn,`cache-invalidation-topic`)].TopicArn' --output text)

echo "✅ SNS topics created"
echo "   📋 SES SNS ARN: $SES_SNS_ARN"
echo "   📋 User Events SNS ARN: $USER_SNS_ARN"
echo "   📋 Campaign Events SNS ARN: $CAMPAIGN_SNS_ARN"
echo "   📋 Cache Invalidation SNS ARN: $CACHE_SNS_ARN"

# Subscribe SQS queues to SNS topics
echo "🔗 Connecting SNS topics to SQS queues..."
aws sns subscribe --topic-arn $SES_SNS_ARN --protocol sqs --notification-endpoint $SES_SQS_ARN
aws sns subscribe --topic-arn $USER_SNS_ARN --protocol sqs --notification-endpoint $NOTIFICATION_SQS_ARN
aws sns subscribe --topic-arn $CAMPAIGN_SNS_ARN --protocol sqs --notification-endpoint $EVENT_SQS_ARN
aws sns subscribe --topic-arn $CACHE_SNS_ARN --protocol sqs --notification-endpoint $CACHE_AWS_SQS_ARN

# Set SQS policies to allow SNS to send messages
echo "🔐 Setting up SQS policies..."
SES_SQS_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": "sqs:SendMessage",
      "Resource": "$SES_SQS_ARN",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "$SES_SNS_ARN"
        }
      }
    }
  ]
}
EOF
)
SES_SQS_POLICY="$(echo "$SES_SQS_POLICY" | tr -d '\n' | tr -s ' ')"

NOTIFICATION_SQS_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": "sqs:SendMessage",
      "Resource": "$NOTIFICATION_SQS_ARN",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "$USER_SNS_ARN"
        }
      }
    }
  ]
}
EOF
)
NOTIFICATION_SQS_POLICY="$(echo "$NOTIFICATION_SQS_POLICY" | tr -d '\n' | tr -s ' ')"

EVENT_SQS_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": "sqs:SendMessage",
      "Resource": "$EVENT_SQS_ARN",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "$CAMPAIGN_SNS_ARN"
        }
      }
    }
  ]
}
EOF
)
EVENT_SQS_POLICY="$(echo "$EVENT_SQS_POLICY" | tr -d '\n' | tr -s ' ')"

CACHE_AWS_SQS_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": "sqs:SendMessage",
      "Resource": "$CACHE_AWS_SQS_ARN",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "$CACHE_SNS_ARN"
        }
      }
    }
  ]
}
EOF
)
CACHE_AWS_SQS_POLICY="$(echo "$CACHE_AWS_SQS_POLICY" | tr -d '\n' | tr -s ' ')"

set_queue_policy "$SES_SQS_URL" "$SES_SQS_POLICY"
set_queue_policy "$NOTIFICATION_SQS_URL" "$NOTIFICATION_SQS_POLICY"
set_queue_policy "$EVENT_SQS_URL" "$EVENT_SQS_POLICY"
set_queue_policy "$CACHE_AWS_SQS_URL" "$CACHE_AWS_SQS_POLICY"

echo "✅ SQS policies configured"

# Create EventBridge custom bus for events
echo "🎯 Setting up EventBridge..."
aws events create-event-bus --name local-event-bus
aws events create-event-bus --name user-event-bus
aws events create-event-bus --name campaign-event-bus

echo "✅ EventBridge buses created"

# Create IAM role for EventBridge Scheduler
echo "🔑 Setting up IAM roles..."
aws iam create-role --role-name LocalEventBridgeSchedulerRole --assume-role-policy-document '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "scheduler.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}'

# Create IAM role for SES
aws iam create-role --role-name LocalSESRole --assume-role-policy-document '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ses.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}'

# Get role ARNs
SCHEDULER_ROLE_ARN=$(aws iam get-role --role-name LocalEventBridgeSchedulerRole --query 'Role.Arn' --output text)
SES_ROLE_ARN=$(aws iam get-role --role-name LocalSESRole --query 'Role.Arn' --output text)

echo "✅ IAM roles created"
echo "   📋 Scheduler Role ARN: $SCHEDULER_ROLE_ARN"
echo "   📋 SES Role ARN: $SES_ROLE_ARN"

# Attach policies to roles
if ! aws iam attach-role-policy --role-name LocalEventBridgeSchedulerRole --policy-arn arn:aws:iam::aws:policy/service-role/AmazonEventBridgeSchedulerFullAccess >/dev/null 2>&1; then
  echo "   ⚠ LocalStack does not provide AWS managed policy AmazonEventBridgeSchedulerFullAccess; skipping attach."
fi
if ! aws iam attach-role-policy --role-name LocalSESRole --policy-arn arn:aws:iam::aws:policy/service-role/AmazonSESFullAccess >/dev/null 2>&1; then
  echo "   ⚠ LocalStack does not provide AWS managed policy AmazonSESFullAccess; skipping attach."
fi

# Create EventBridge Scheduler schedule groups
echo "📅 Setting up EventBridge Scheduler..."
aws scheduler create-schedule-group --name local-schedule-group
aws scheduler create-schedule-group --name notification-schedule-group
aws scheduler create-schedule-group --name campaign-schedule-group

echo "✅ EventBridge Scheduler groups created"

# Create EventBridge rules for different event patterns
echo "📋 Setting up EventBridge rules..."

# Rule for user events
aws events put-rule \
    --event-bus-name user-event-bus \
    --name user-activity-rule \
    --event-pattern '{"source":["local.user"],"detail-type":["User Activity","User Registration","User Update"]}'

# Rule for campaign events
aws events put-rule \
    --event-bus-name campaign-event-bus \
    --name campaign-activity-rule \
    --event-pattern '{"source":["local.campaign"],"detail-type":["Campaign Created","Campaign Updated","Event Created"]}'

# Rule for email events
aws events put-rule \
    --event-bus-name local-event-bus \
    --name email-activity-rule \
    --event-pattern '{"source":["local.email"],"detail-type":["Email Sent","Email Delivered","Email Bounced"]}'

echo "✅ EventBridge rules created"

# Create targets for the rules (connect to SQS queues)
echo "🎯 Setting up EventBridge targets..."

# User events -> Notification queue
aws events put-targets \
    --event-bus-name user-event-bus \
    --rule user-activity-rule \
    --targets "Id"="1","Arn"="$NOTIFICATION_SQS_ARN"

# Campaign events -> Event processing queue
aws events put-targets \
    --event-bus-name campaign-event-bus \
    --rule campaign-activity-rule \
    --targets "Id"="1","Arn"="$EVENT_SQS_ARN"

# Email events -> SES queue
aws events put-targets \
    --event-bus-name local-event-bus \
    --rule email-activity-rule \
    --targets "Id"="1","Arn"="$SES_SQS_ARN"

echo "✅ EventBridge targets configured"

# Test EventBridge by sending a sample event
echo "🧪 Testing EventBridge with sample events..."
aws events put-events \
    --entries '[
        {
            "Source": "local.user",
            "DetailType": "User Registration", 
            "Detail": "{\"userId\":\"test-001\",\"email\":\"test@example.com\",\"timestamp\":\"'$(date -u +%Y-%m-%dT%H:%M:%SZ)'\"}",
            "EventBusName": "user-event-bus"
        }
    ]'

aws events put-events \
    --entries '[
        {
            "Source": "local.email",
            "DetailType": "Email Sent",
            "Detail": "{\"messageId\":\"test-msg-001\",\"recipient\":\"test@example.com\",\"subject\":\"Test Email\",\"timestamp\":\"'$(date -u +%Y-%m-%dT%H:%M:%SZ)'\"}",
            "EventBusName": "local-event-bus"
        }
    ]'

echo "✅ Sample events sent to EventBridge"

echo ""
echo "🎉 LocalStack AWS services setup completed successfully!"
echo ""
echo "📋 Summary of created resources:"
echo ""
echo "📧 SES (Simple Email Service):"
echo "   ✓ Verified emails: test@example.com, notification@example.com, noreply@local.dev, admin@local.dev"
echo "   ✓ Rules: user-activity-rule, campaign-activity-rule, email-activity-rule"
echo ""
echo "📨 SQS (Simple Queue Service):"
echo "   ✓ ses_sqs ($SES_SQS_ARN)"
echo "   ✓ notification_queue ($NOTIFICATION_SQS_ARN)"
echo "   ✓ event_processing_queue ($EVENT_SQS_ARN)"
echo "   ✓ crm-dr-cache-invalidation-queue-aws ($CACHE_AWS_SQS_ARN)"
echo ""
echo "📢 SNS (Simple Notification Service):"
echo "   ✓ ses-events-topic ($SES_SNS_ARN)"
echo "   ✓ user-events-topic ($USER_SNS_ARN)"
echo "   ✓ campaign-events-topic ($CAMPAIGN_SNS_ARN)"
echo "   ✓ cache-invalidation-topic ($CACHE_SNS_ARN)"
echo ""
echo "🎯 EventBridge:"
echo "   ✓ Event buses: local-event-bus, user-event-bus, campaign-event-bus"
echo "   ✓ Rules: user-activity, campaign-activity, email-activity"
echo ""
echo "📅 EventBridge Scheduler:"
echo "   ✓ Schedule groups: local-schedule-group, notification-schedule-group, campaign-schedule-group"
echo ""
echo "🔑 IAM Roles:"
echo "   ✓ LocalEventBridgeSchedulerRole ($SCHEDULER_ROLE_ARN)"
echo "   ✓ LocalSESRole ($SES_ROLE_ARN)"
echo ""
echo "🔧 Configuration for application-local.yml:"
echo "spring:"
echo "  aws:"
echo "    endpoint-url: http://localhost:4566"
echo "    credentials:"
echo "      access-key: test"
echo "      secret-key: test"
echo "    region: ap-northeast-2"
echo "    mail:"
echo "      configuration-set:"
echo "        default: local-configuration-set"
echo "    schedule:"
echo "      role-arn: $SCHEDULER_ROLE_ARN"
echo "      sqs-arn: $SES_SQS_ARN"
echo "      group-name: local-schedule-group"
echo ""
echo "🔧 Environment variables needed:"
echo "AWS_SNS_CACHE_INVALIDATION_TOPIC_ARN=$CACHE_SNS_ARN"
echo ""
echo "🌐 LocalStack Web UI: http://localhost:4566"
echo "📊 LocalStack Health Check: http://localhost:4566/health"
echo ""
echo "🚀 Ready for local development with full AWS services mocking!"
