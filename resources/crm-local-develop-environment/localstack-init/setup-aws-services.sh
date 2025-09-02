#!/bin/bash

# LocalStack AWS Services Setup Script for Local Development

echo "🚀 Setting up AWS services for local development environment..."

# Wait for LocalStack to be ready
echo "⏳ Waiting for LocalStack to be ready..."
while ! curl -s http://localhost:4566/health > /dev/null; do
  echo "   Still waiting for LocalStack..."
  sleep 3
done

echo "✅ LocalStack is ready. Setting up services..."

# Create SES identity for local development
echo "📧 Setting up SES (Simple Email Service)..."
awslocal ses verify-email-identity --email-address test@example.com
awslocal ses verify-email-identity --email-address notification@example.com
awslocal ses verify-email-identity --email-address noreply@local.dev
awslocal ses verify-email-identity --email-address admin@local.dev

# Create SES configuration set for local development
awslocal ses create-configuration-set --configuration-set Name=local-configuration-set

echo "✅ SES email identities created"

# Create SQS queue for SES events and general messaging
echo "📨 Setting up SQS (Simple Queue Service)..."
awslocal sqs create-queue --queue-name ses_sqs
awslocal sqs create-queue --queue-name notification_queue
awslocal sqs create-queue --queue-name event_processing_queue

# Get SQS queue URLs and ARNs
SES_SQS_URL=$(awslocal sqs get-queue-url --queue-name ses_sqs --query 'QueueUrl' --output text)
SES_SQS_ARN=$(awslocal sqs get-queue-attributes --queue-url $SES_SQS_URL --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

NOTIFICATION_SQS_URL=$(awslocal sqs get-queue-url --queue-name notification_queue --query 'QueueUrl' --output text)
NOTIFICATION_SQS_ARN=$(awslocal sqs get-queue-attributes --queue-url $NOTIFICATION_SQS_URL --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

EVENT_SQS_URL=$(awslocal sqs get-queue-url --queue-name event_processing_queue --query 'QueueUrl' --output text)
EVENT_SQS_ARN=$(awslocal sqs get-queue-attributes --queue-url $EVENT_SQS_URL --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

echo "✅ SQS queues created"
echo "   📋 SES SQS ARN: $SES_SQS_ARN"
echo "   📋 Notification SQS ARN: $NOTIFICATION_SQS_ARN"
echo "   📋 Event Processing SQS ARN: $EVENT_SQS_ARN"

# Create SNS topics for different types of notifications
echo "📢 Setting up SNS (Simple Notification Service)..."
awslocal sns create-topic --name ses-events-topic
awslocal sns create-topic --name user-events-topic
awslocal sns create-topic --name campaign-events-topic

# Get SNS topic ARNs
SES_SNS_ARN=$(awslocal sns list-topics --query 'Topics[?contains(TopicArn,`ses-events-topic`)].TopicArn' --output text)
USER_SNS_ARN=$(awslocal sns list-topics --query 'Topics[?contains(TopicArn,`user-events-topic`)].TopicArn' --output text)
CAMPAIGN_SNS_ARN=$(awslocal sns list-topics --query 'Topics[?contains(TopicArn,`campaign-events-topic`)].TopicArn' --output text)

echo "✅ SNS topics created"
echo "   📋 SES SNS ARN: $SES_SNS_ARN"
echo "   📋 User Events SNS ARN: $USER_SNS_ARN"
echo "   📋 Campaign Events SNS ARN: $CAMPAIGN_SNS_ARN"

# Subscribe SQS queues to SNS topics
echo "🔗 Connecting SNS topics to SQS queues..."
awslocal sns subscribe --topic-arn $SES_SNS_ARN --protocol sqs --notification-endpoint $SES_SQS_ARN
awslocal sns subscribe --topic-arn $USER_SNS_ARN --protocol sqs --notification-endpoint $NOTIFICATION_SQS_ARN
awslocal sns subscribe --topic-arn $CAMPAIGN_SNS_ARN --protocol sqs --notification-endpoint $EVENT_SQS_ARN

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

awslocal sqs set-queue-attributes --queue-url $SES_SQS_URL --attributes Policy="$SES_SQS_POLICY"
awslocal sqs set-queue-attributes --queue-url $NOTIFICATION_SQS_URL --attributes Policy="$NOTIFICATION_SQS_POLICY"
awslocal sqs set-queue-attributes --queue-url $EVENT_SQS_URL --attributes Policy="$EVENT_SQS_POLICY"

echo "✅ SQS policies configured"

# Create EventBridge custom bus for events
echo "🎯 Setting up EventBridge..."
awslocal events create-event-bus --name local-event-bus
awslocal events create-event-bus --name user-event-bus
awslocal events create-event-bus --name campaign-event-bus

echo "✅ EventBridge buses created"

# Create IAM role for EventBridge Scheduler
echo "🔑 Setting up IAM roles..."
awslocal iam create-role --role-name LocalEventBridgeSchedulerRole --assume-role-policy-document '{
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
awslocal iam create-role --role-name LocalSESRole --assume-role-policy-document '{
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
SCHEDULER_ROLE_ARN=$(awslocal iam get-role --role-name LocalEventBridgeSchedulerRole --query 'Role.Arn' --output text)
SES_ROLE_ARN=$(awslocal iam get-role --role-name LocalSESRole --query 'Role.Arn' --output text)

echo "✅ IAM roles created"
echo "   📋 Scheduler Role ARN: $SCHEDULER_ROLE_ARN"
echo "   📋 SES Role ARN: $SES_ROLE_ARN"

# Attach policies to roles
awslocal iam attach-role-policy --role-name LocalEventBridgeSchedulerRole --policy-arn arn:aws:iam::aws:policy/service-role/AmazonEventBridgeSchedulerFullAccess
awslocal iam attach-role-policy --role-name LocalSESRole --policy-arn arn:aws:iam::aws:policy/service-role/AmazonSESFullAccess

# Create EventBridge Scheduler schedule groups
echo "📅 Setting up EventBridge Scheduler..."
awslocal scheduler create-schedule-group --name local-schedule-group
awslocal scheduler create-schedule-group --name notification-schedule-group
awslocal scheduler create-schedule-group --name campaign-schedule-group

echo "✅ EventBridge Scheduler groups created"

# Create EventBridge rules for different event patterns
echo "📋 Setting up EventBridge rules..."

# Rule for user events
awslocal events put-rule \
    --event-bus-name user-event-bus \
    --name user-activity-rule \
    --event-pattern '{"source":["local.user"],"detail-type":["User Activity","User Registration","User Update"]}'

# Rule for campaign events
awslocal events put-rule \
    --event-bus-name campaign-event-bus \
    --name campaign-activity-rule \
    --event-pattern '{"source":["local.campaign"],"detail-type":["Campaign Created","Campaign Updated","Event Created"]}'

# Rule for email events
awslocal events put-rule \
    --event-bus-name local-event-bus \
    --name email-activity-rule \
    --event-pattern '{"source":["local.email"],"detail-type":["Email Sent","Email Delivered","Email Bounced"]}'

echo "✅ EventBridge rules created"

# Create targets for the rules (connect to SQS queues)
echo "🎯 Setting up EventBridge targets..."

# User events -> Notification queue
awslocal events put-targets \
    --event-bus-name user-event-bus \
    --rule user-activity-rule \
    --targets "Id"="1","Arn"="$NOTIFICATION_SQS_ARN"

# Campaign events -> Event processing queue
awslocal events put-targets \
    --event-bus-name campaign-event-bus \
    --rule campaign-activity-rule \
    --targets "Id"="1","Arn"="$EVENT_SQS_ARN"

# Email events -> SES queue
awslocal events put-targets \
    --event-bus-name local-event-bus \
    --rule email-activity-rule \
    --targets "Id"="1","Arn"="$SES_SQS_ARN"

echo "✅ EventBridge targets configured"

# Test EventBridge by sending a sample event
echo "🧪 Testing EventBridge with sample events..."
awslocal events put-events \
    --entries '[
        {
            "Source": "local.user",
            "DetailType": "User Registration", 
            "Detail": "{\"userId\":\"test-001\",\"email\":\"test@example.com\",\"timestamp\":\"'$(date -u +%Y-%m-%dT%H:%M:%SZ)'\"}"
        }
    ]' \
    --event-bus-name user-event-bus

awslocal events put-events \
    --entries '[
        {
            "Source": "local.email",
            "DetailType": "Email Sent",
            "Detail": "{\"messageId\":\"test-msg-001\",\"recipient\":\"test@example.com\",\"subject\":\"Test Email\",\"timestamp\":\"'$(date -u +%Y-%m-%dT%H:%M:%SZ)'\"}"
        }
    ]' \
    --event-bus-name local-event-bus

echo "✅ Sample events sent to EventBridge"

echo ""
echo "🎉 LocalStack AWS services setup completed successfully!"
echo ""
echo "📋 Summary of created resources:"
echo ""
echo "📧 SES (Simple Email Service):"
echo "   ✓ Verified emails: test@example.com, notification@example.com, noreply@local, admin@local"
echo "   ✓ Configuration set: local-configuration-set"
echo ""
echo "📨 SQS (Simple Queue Service):"
echo "   ✓ ses_sqs ($SES_SQS_ARN)"
echo "   ✓ notification_queue ($NOTIFICATION_SQS_ARN)"
echo "   ✓ event_processing_queue ($EVENT_SQS_ARN)"
echo ""
echo "📢 SNS (Simple Notification Service):"
echo "   ✓ ses-events-topic ($SES_SNS_ARN)"
echo "   ✓ user-events-topic ($USER_SNS_ARN)"
echo "   ✓ campaign-events-topic ($CAMPAIGN_SNS_ARN)"
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
echo "🌐 LocalStack Web UI: http://localhost:4566"
echo "📊 LocalStack Health Check: http://localhost:4566/health"
echo ""
echo "🚀 Ready for local development with full AWS services mocking!"