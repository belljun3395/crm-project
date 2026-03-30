output "cache_invalidation_topic_arn" {
  description = "ARN of the SNS topic used for cache invalidation."
  value       = aws_sns_topic.cache_invalidation.arn
}

output "cache_invalidation_queue_arn" {
  description = "ARN of the SQS queue used for cache invalidation."
  value       = aws_sqs_queue.cache_invalidation.arn
}

output "schedule_queue_arn" {
  description = "ARN of the SQS queue used by EventBridge Scheduler."
  value       = aws_sqs_queue.schedule_event.arn
}

output "ses_queue_arn" {
  description = "ARN of the SQS queue used for SES notifications."
  value       = aws_sqs_queue.ses.arn
}

output "schedule_group_name" {
  description = "EventBridge Scheduler group name."
  value       = aws_scheduler_schedule_group.this.name
}

output "schedule_role_arn" {
  description = "IAM role ARN assumed by EventBridge Scheduler."
  value       = aws_iam_role.scheduler_execution.arn
}

output "ses_configuration_set_name" {
  description = "SES configuration set name."
  value       = aws_ses_configuration_set.this.name
}

output "runtime_access_key_id" {
  description = "Access key ID used by the application runtime."
  value       = aws_iam_access_key.runtime.id
}

output "runtime_secret_access_key" {
  description = "Secret access key used by the application runtime."
  value       = aws_iam_access_key.runtime.secret
  sensitive   = true
}

