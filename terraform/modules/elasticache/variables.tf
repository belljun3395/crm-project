variable "cluster_id" {
  description = "Identifier for the ElastiCache replication group"
  type        = string
}

variable "description" {
  description = "Description of the replication group"
  type        = string
  default     = "ElastiCache Redis Cluster"
}

variable "engine_version" {
  description = "Redis engine version"
  type        = string
  default     = "7.0"
}

variable "node_type" {
  description = "Instance type for cache nodes"
  type        = string
  default     = "cache.t3.micro"
}

variable "num_cache_clusters" {
  description = "Number of cache clusters (nodes) - used when cluster mode is disabled"
  type        = number
  default     = 2
}

variable "port" {
  description = "Redis port"
  type        = number
  default     = 6379
}

variable "cluster_mode_enabled" {
  description = "Enable cluster mode (sharding)"
  type        = bool
  default     = false
}

variable "num_node_groups" {
  description = "Number of node groups (shards) - used when cluster mode is enabled"
  type        = number
  default     = 1
}

variable "replicas_per_node_group" {
  description = "Number of replica nodes per node group - used when cluster mode is enabled"
  type        = number
  default     = 1
}

variable "vpc_id" {
  description = "VPC ID where the ElastiCache cluster will be created"
  type        = string
}

variable "subnet_ids" {
  description = "List of subnet IDs for the cache subnet group"
  type        = list(string)
}

variable "allowed_cidr_blocks" {
  description = "List of CIDR blocks allowed to access the ElastiCache cluster"
  type        = list(string)
}

variable "create_parameter_group" {
  description = "Whether to create a parameter group"
  type        = bool
  default     = true
}

variable "parameter_group_family" {
  description = "Parameter group family"
  type        = string
  default     = "redis7"
}

variable "parameter_group_name" {
  description = "Name of existing parameter group to use (if create_parameter_group is false)"
  type        = string
  default     = null
}

variable "parameters" {
  description = "List of parameters for the parameter group"
  type = list(object({
    name  = string
    value = string
  }))
  default = []
}

variable "auth_token_enabled" {
  description = "Enable auth token (password) for Redis"
  type        = bool
  default     = true
}

variable "auth_token" {
  description = "Auth token (password) for Redis - must be 16-128 characters"
  type        = string
  default     = null
  sensitive   = true
}

variable "transit_encryption_enabled" {
  description = "Enable encryption in transit"
  type        = bool
  default     = true
}

variable "at_rest_encryption_enabled" {
  description = "Enable encryption at rest"
  type        = bool
  default     = true
}

variable "kms_key_id" {
  description = "KMS key ID for at-rest encryption"
  type        = string
  default     = null
}

variable "snapshot_retention_limit" {
  description = "Number of days to retain snapshots"
  type        = number
  default     = 7
}

variable "snapshot_window" {
  description = "Preferred snapshot window"
  type        = string
  default     = "03:00-04:00"
}

variable "maintenance_window" {
  description = "Preferred maintenance window"
  type        = string
  default     = "sun:04:00-sun:05:00"
}

variable "notification_topic_arn" {
  description = "ARN of SNS topic for ElastiCache notifications"
  type        = string
  default     = null
}

variable "automatic_failover_enabled" {
  description = "Enable automatic failover"
  type        = bool
  default     = true
}

variable "multi_az_enabled" {
  description = "Enable Multi-AZ deployment"
  type        = bool
  default     = true
}

variable "auto_minor_version_upgrade" {
  description = "Enable automatic minor version upgrades"
  type        = bool
  default     = true
}

variable "apply_immediately" {
  description = "Apply changes immediately"
  type        = bool
  default     = false
}

variable "log_delivery_configuration" {
  description = "Log delivery configuration for slow-log and engine-log"
  type = list(object({
    destination      = string
    destination_type = string
    log_format       = string
    log_type         = string
  }))
  default = []
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
