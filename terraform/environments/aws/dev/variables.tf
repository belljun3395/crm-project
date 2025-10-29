variable "aws_region" {
  description = "AWS region where resources will be provisioned."
  type        = string
  default     = "ap-northeast-2"
}

variable "environment" {
  description = "Environment name used for tagging."
  type        = string
  default     = "dev"
}

variable "project" {
  description = "Project identifier used for tagging."
  type        = string
  default     = "crm"
}

variable "cluster_name" {
  description = "EKS cluster name. Also used as prefix for networking resources."
  type        = string
  default     = "crm-dev"
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
  default     = ["t3.medium"]
}

variable "node_group_desired_capacity" {
  description = "Desired node count."
  type        = number
  default     = 2
}

variable "node_group_min_size" {
  description = "Minimum node count."
  type        = number
  default     = 1
}

variable "node_group_max_size" {
  description = "Maximum node count."
  type        = number
  default     = 3
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
