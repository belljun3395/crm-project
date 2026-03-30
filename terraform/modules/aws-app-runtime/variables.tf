variable "name_prefix" {
  description = "Prefix used for IAM and scheduling resource names."
  type        = string
}

variable "tags" {
  description = "Tags to apply to supported resources."
  type        = map(string)
  default     = {}
}

variable "cache_invalidation_topic_name" {
  description = "SNS topic name used for cache invalidation events."
  type        = string
  default     = null
}

variable "cache_invalidation_queue_name" {
  description = "SQS queue name used to receive cache invalidation events."
  type        = string
  default     = "crm-dr-cache-invalidation-queue-aws"
}

variable "schedule_queue_name" {
  description = "SQS queue name used as the target for EventBridge Scheduler."
  type        = string
  default     = "crm_schedule_event_sqs"
}

variable "ses_queue_name" {
  description = "SQS queue name used for SES notifications."
  type        = string
  default     = "crm_ses_sqs"
}

variable "schedule_group_name" {
  description = "EventBridge Scheduler group name."
  type        = string
  default     = null
}

variable "schedule_role_name" {
  description = "IAM role name assumed by EventBridge Scheduler."
  type        = string
  default     = null
}

variable "ses_configuration_set_name" {
  description = "SES configuration set name used by the application."
  type        = string
  default     = null
}

variable "runtime_user_name" {
  description = "IAM user name used by the application runtime."
  type        = string
  default     = null
}
