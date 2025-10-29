variable "name" {
  description = "Prefix that will be used for tagging all networking resources."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
}

variable "azs" {
  description = "List of availability zones to spread subnets across."
  type        = list(string)
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for the public subnets. Must align with azs order."
  type        = list(string)
  validation {
    condition     = length(var.public_subnet_cidrs) == length(var.azs)
    error_message = "public_subnet_cidrs must have the same number of entries as azs."
  }
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for the private subnets. Must align with azs order."
  type        = list(string)
  validation {
    condition     = length(var.private_subnet_cidrs) == length(var.azs)
    error_message = "private_subnet_cidrs must have the same number of entries as azs."
  }
}

variable "enable_nat" {
  description = "Whether to provision a single NAT gateway for the private subnets."
  type        = bool
  default     = true
}

variable "tags" {
  description = "Common tags applied to all resources."
  type        = map(string)
  default     = {}
}

variable "create_flow_logs" {
  description = "Enable VPC flow logs when true."
  type        = bool
  default     = false
}

variable "flow_log_destination_arn" {
  description = "CloudWatch Logs group or S3 bucket ARN for flow logs. Required when create_flow_logs is true."
  type        = string
  default     = null
  validation {
    condition     = !var.create_flow_logs || var.flow_log_destination_arn != null
    error_message = "flow_log_destination_arn must be set when create_flow_logs is true."
  }
}

variable "flow_log_destination_type" {
  description = "Destination type for flow logs. One of \"s3\" or \"cloud-watch-logs\" when create_flow_logs is true."
  type        = string
  default     = null
  validation {
    condition     = !var.create_flow_logs || contains(["s3", "cloud-watch-logs"], var.flow_log_destination_type)
    error_message = "flow_log_destination_type must be \"s3\" or \"cloud-watch-logs\" when create_flow_logs is true."
  }
}
