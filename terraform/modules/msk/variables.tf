variable "name_prefix" {
  description = "Prefix for resource names"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "client_subnets" {
  description = "List of private subnet IDs for brokers"
  type        = list(string)
}

variable "allowed_cidr_blocks" {
  description = "CIDR blocks allowed to access MSK"
  type        = list(string)
}

variable "kafka_version" {
  description = "Kafka version"
  type        = string
  default     = "3.6.0"
}

variable "number_of_broker_nodes" {
  description = "Number of broker nodes"
  type        = number
  default     = 2
}

variable "instance_type" {
  description = "Broker instance type"
  type        = string
  default     = "kafka.t3.small"
}

variable "volume_size" {
  description = "EBS volume size per broker (GB)"
  type        = number
  default     = 10
}

variable "kms_key_arn" {
  description = "KMS Key ARN for encryption at rest"
  type        = string
  default     = ""
}

variable "scram_secret_arn_list" {
  description = "List of Secrets Manager ARNs for SCRAM authentication"
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "Tags"
  type        = map(string)
  default     = {}
}
