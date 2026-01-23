variable "aws_region" {
  description = "AWS region where resources will be provisioned."
  type        = string
  default     = "ap-northeast-2"
}

variable "environment" {
  description = "Environment name used for tagging."
  type        = string
  default     = "dr"
}

variable "project" {
  description = "Project identifier used for tagging."
  type        = string
  default     = "crm"
}

variable "cluster_name" {
  description = "EKS cluster name. Also used as prefix for networking resources."
  type        = string
  default     = "crm-dr"
}

variable "vpc_cidr" {
  description = "CIDR block assigned to the VPC."
  type        = string
  default     = "10.10.0.0/16"
}

variable "availability_zones" {
  description = "List of availability zones to spread subnets across."
  type        = list(string)
  default     = ["ap-northeast-2a", "ap-northeast-2c"]
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for the public subnets."
  type        = list(string)
  default     = ["10.10.0.0/24", "10.10.1.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for the private subnets."
  type        = list(string)
  default     = ["10.10.10.0/24", "10.10.11.0/24"]
}

variable "enable_nat_gateway" {
  description = "Whether to create a NAT gateway for the private subnets."
  type        = bool
  default     = true
}

variable "cluster_version" {
  description = "Desired Kubernetes version for the EKS control plane."
  type        = string
  default     = "1.29"
}

variable "enable_cluster_public_access" {
  description = "Expose the EKS API server to the internet. Useful for dev but restrict CIDRs."
  type        = bool
  default     = true
}

variable "cluster_public_access_cidrs" {
  description = "CIDR blocks allowed to reach the public API endpoint."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "cluster_enabled_log_types" {
  description = "Control plane log types to enable."
  type        = list(string)
  default     = ["api", "audit", "authenticator"]
}

variable "node_group_instance_types" {
  description = "Instance types for the default node group."
  type        = list(string)
  default     = ["t3.xlarge"]
}

variable "node_group_desired_capacity" {
  description = "Desired node count."
  type        = number
  default     = 5
}

variable "node_group_min_size" {
  description = "Minimum node count."
  type        = number
  default     = 3
}

variable "node_group_max_size" {
  description = "Maximum node count."
  type        = number
  default     = 10
}

variable "node_group_capacity_type" {
  description = "Capacity type for nodes."
  type        = string
  default     = "ON_DEMAND"
}

variable "ecr_repository_name" {
  description = "ECR repository name for application images."
  type        = string
  default     = "crm-app"
}

variable "secrets_manager_secret_name" {
  description = "Override name for the Secrets Manager secret. Defaults to /<project>/<environment>/application"
  type        = string
  default     = null
}

variable "secrets_manager_description" {
  description = "Description attached to the Secrets Manager secret."
  type        = string
  default     = null
}

variable "secrets_manager_kms_key_id" {
  description = "KMS key ID or ARN used to encrypt the secret."
  type        = string
  default     = null
}

variable "secrets_manager_recovery_window_in_days" {
  description = "Number of days before a deleted secret is permanently removed."
  type        = number
  default     = 7
}

variable "app_env" {
  description = "Application runtime environment variables that will be stored in AWS Secrets Manager."
  type = object({
    DATABASE_URL            = string
    DATABASE_USERNAME       = string
    DATABASE_PASSWORD       = string
    REDIS_HOST              = string
    REDIS_MAX_REDIRECTS     = string
    REDIS_PASSWORD          = string
    REDIS_NODES             = string
    MAIL_USERNAME           = string
    MAIL_PASSWORD           = string
    AWS_ACCESS_KEY          = string
    AWS_SECRET_KEY          = string
    AWS_CONFIGURATION_SET   = string
    AWS_SCHEDULE_ROLE_ARN   = string
    AWS_SCHEDULE_SQS_ARN    = string
    AWS_SCHEDULE_GROUP_NAME = string
    KAFKA_BOOTSTRAP_SERVERS = string
    SCHEDULER_PROVIDER      = optional(string, "aws")
  })
}

variable "additional_secret_values" {
  description = "Additional key/value pairs to merge into the application secret."
  type        = map(string)
  default     = {}
  validation {
    condition     = length(setintersection(toset(keys(var.additional_secret_values)), toset(keys(var.app_env)))) == 0
    error_message = "additional_secret_values keys must not overlap with app_env keys."
  }
}

# RDS Variables
variable "enable_rds" {
  description = "Enable RDS PostgreSQL database"
  type        = bool
  default     = true
}

variable "rds_engine_version" {
  description = "PostgreSQL engine version"
  type        = string
  default     = "15.4"
}

variable "rds_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.medium"
}

variable "rds_allocated_storage" {
  description = "Allocated storage in GB"
  type        = number
  default     = 100
}

variable "rds_storage_encrypted" {
  description = "Enable storage encryption"
  type        = bool
  default     = true
}

variable "rds_kms_key_id" {
  description = "KMS key ID for RDS encryption"
  type        = string
  default     = null
}

variable "rds_database_name" {
  description = "Name of the default database"
  type        = string
  default     = "crm"
}

variable "rds_master_username" {
  description = "Master username for RDS"
  type        = string
  default     = "crmadmin"
}

variable "rds_master_password" {
  description = "Master password for RDS"
  type        = string
  sensitive   = true
  default     = null
}

variable "rds_allowed_cidr_blocks" {
  description = "CIDR blocks allowed to access RDS"
  type        = list(string)
  default     = []
}

variable "rds_multi_az" {
  description = "Enable Multi-AZ deployment"
  type        = bool
  default     = true
}

variable "rds_backup_retention_period" {
  description = "Backup retention period in days"
  type        = number
  default     = 30
}

variable "rds_skip_final_snapshot" {
  description = "Skip final snapshot on deletion"
  type        = bool
  default     = false
}

variable "rds_deletion_protection" {
  description = "Enable deletion protection"
  type        = bool
  default     = true
}

# ElastiCache Variables
variable "enable_elasticache" {
  description = "Enable ElastiCache Redis cluster"
  type        = bool
  default     = true
}

variable "elasticache_engine_version" {
  description = "Redis engine version"
  type        = string
  default     = "7.0"
}

variable "elasticache_node_type" {
  description = "ElastiCache node type"
  type        = string
  default     = "cache.t3.medium"
}

variable "elasticache_num_cache_clusters" {
  description = "Number of cache clusters"
  type        = number
  default     = 3
}

variable "elasticache_allowed_cidr_blocks" {
  description = "CIDR blocks allowed to access ElastiCache"
  type        = list(string)
  default     = []
}

variable "elasticache_auth_token_enabled" {
  description = "Enable auth token"
  type        = bool
  default     = true
}

variable "elasticache_auth_token" {
  description = "Auth token for Redis"
  type        = string
  sensitive   = true
  default     = null
}

variable "elasticache_transit_encryption_enabled" {
  description = "Enable transit encryption"
  type        = bool
  default     = true
}

variable "elasticache_at_rest_encryption_enabled" {
  description = "Enable at-rest encryption"
  type        = bool
  default     = true
}

variable "elasticache_snapshot_retention_limit" {
  description = "Number of days to retain snapshots"
  type        = number
  default     = 30
}

variable "elasticache_automatic_failover_enabled" {
  description = "Enable automatic failover"
  type        = bool
  default     = true
}

variable "elasticache_multi_az_enabled" {
  description = "Enable Multi-AZ deployment"
  type        = bool
  default     = true
}

# VPN Configuration
variable "enable_vpn" {
  description = "Enable VPN connection to GCP"
  type        = bool
  default     = true
}

variable "gcp_project_id" {
  description = "GCP Project ID for VPN connection"
  type        = string
}

variable "gcp_network_id" {
  description = "GCP VPC network ID for VPN"
  type        = string
  default     = ""
}

variable "gcp_region" {
  description = "GCP region for VPN"
  type        = string
  default     = "asia-northeast3"
}

variable "gcp_vpc_cidr" {
  description = "GCP VPC CIDR for security group rules"
  type        = string
  default     = "10.20.0.0/20"
}

variable "vpn_tunnel1_preshared_key" {
  description = "Pre-shared key for VPN tunnel 1"
  type        = string
  sensitive   = true
  default     = ""
}

variable "vpn_tunnel2_preshared_key" {
  description = "Pre-shared key for VPN tunnel 2"
  type        = string
  sensitive   = true
  default     = ""
}

# ALB Configuration
variable "alb_deletion_protection" {
  description = "Enable ALB deletion protection"
  type        = bool
  default     = true
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for HTTPS"
  type        = string
  default     = ""
}

# Kafka Configuration
variable "enable_kafka" {
  description = "Enable MSK Kafka cluster"
  type        = bool
  default     = true
}

variable "kafka_version" {
  description = "Kafka version"
  type        = string
  default     = "3.6.0"
}

variable "kafka_instance_type" {
  description = "Kafka broker instance type"
  type        = string
  default     = "kafka.t3.small"
}

variable "kafka_number_of_brokers" {
  description = "Number of Kafka brokers"
  type        = number
  default     = 2
}

variable "kafka_volume_size" {
  description = "EBS volume size per broker (GB)"
  type        = number
  default     = 10
}

variable "kafka_allowed_cidr_blocks" {
  description = "CIDR blocks allowed to access Kafka"
  type        = list(string)
  default     = []
}
