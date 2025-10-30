variable "instance_id" {
  description = "Unique identifier for the Redis instance"
  type        = string
}

variable "display_name" {
  description = "Display name for the Redis instance"
  type        = string
  default     = null
}

variable "tier" {
  description = "Service tier of the instance (BASIC or STANDARD_HA)"
  type        = string
  default     = "STANDARD_HA"
  validation {
    condition     = contains(["BASIC", "STANDARD_HA"], var.tier)
    error_message = "Tier must be either BASIC or STANDARD_HA."
  }
}

variable "memory_size_gb" {
  description = "Redis memory size in GiB"
  type        = number
  default     = 1
  validation {
    condition     = var.memory_size_gb >= 1 && var.memory_size_gb <= 300
    error_message = "Memory size must be between 1 and 300 GiB."
  }
}

variable "region" {
  description = "GCP region for the Redis instance"
  type        = string
}

variable "redis_version" {
  description = "Redis version (e.g., REDIS_7_0, REDIS_6_X, REDIS_5_0)"
  type        = string
  default     = "REDIS_7_0"
}

variable "replica_count" {
  description = "Number of replica nodes (0-5, only for STANDARD_HA tier)"
  type        = number
  default     = 1
  validation {
    condition     = var.replica_count >= 0 && var.replica_count <= 5
    error_message = "Replica count must be between 0 and 5."
  }
}

variable "read_replicas_mode" {
  description = "Read replicas mode (READ_REPLICAS_DISABLED or READ_REPLICAS_ENABLED)"
  type        = string
  default     = "READ_REPLICAS_DISABLED"
  validation {
    condition     = contains(["READ_REPLICAS_DISABLED", "READ_REPLICAS_ENABLED"], var.read_replicas_mode)
    error_message = "Read replicas mode must be READ_REPLICAS_DISABLED or READ_REPLICAS_ENABLED."
  }
}

variable "authorized_network" {
  description = "VPC network to attach the Redis instance to"
  type        = string
}

variable "connect_mode" {
  description = "Connection mode (DIRECT_PEERING or PRIVATE_SERVICE_ACCESS)"
  type        = string
  default     = "DIRECT_PEERING"
  validation {
    condition     = contains(["DIRECT_PEERING", "PRIVATE_SERVICE_ACCESS"], var.connect_mode)
    error_message = "Connect mode must be DIRECT_PEERING or PRIVATE_SERVICE_ACCESS."
  }
}

variable "reserved_ip_range" {
  description = "CIDR range of internal addresses reserved for this instance"
  type        = string
  default     = null
}

variable "auth_enabled" {
  description = "Enable Redis AUTH for the instance"
  type        = bool
  default     = true
}

variable "transit_encryption_mode" {
  description = "TLS mode (SERVER_AUTHENTICATION or DISABLED)"
  type        = string
  default     = "SERVER_AUTHENTICATION"
  validation {
    condition     = contains(["SERVER_AUTHENTICATION", "DISABLED"], var.transit_encryption_mode)
    error_message = "Transit encryption mode must be SERVER_AUTHENTICATION or DISABLED."
  }
}

variable "redis_configs" {
  description = "Redis configuration parameters"
  type        = map(string)
  default     = {}
}

variable "maintenance_day" {
  description = "Day of week for maintenance (MON, TUE, WED, THU, FRI, SAT, SUN)"
  type        = string
  default     = "SUN"
  validation {
    condition     = contains(["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"], var.maintenance_day)
    error_message = "Maintenance day must be a valid day of week."
  }
}

variable "maintenance_start_hour" {
  description = "Hour of day (0-23) for maintenance start time"
  type        = number
  default     = 4
  validation {
    condition     = var.maintenance_start_hour >= 0 && var.maintenance_start_hour <= 23
    error_message = "Maintenance start hour must be between 0 and 23."
  }
}

variable "maintenance_start_minute" {
  description = "Minute of hour (0-59) for maintenance start time"
  type        = number
  default     = 0
  validation {
    condition     = var.maintenance_start_minute >= 0 && var.maintenance_start_minute <= 59
    error_message = "Maintenance start minute must be between 0 and 59."
  }
}

variable "persistence_mode" {
  description = "Persistence mode (DISABLED or RDB)"
  type        = string
  default     = "RDB"
  validation {
    condition     = contains(["DISABLED", "RDB"], var.persistence_mode)
    error_message = "Persistence mode must be DISABLED or RDB."
  }
}

variable "rdb_snapshot_period" {
  description = "RDB snapshot period (ONE_HOUR, SIX_HOURS, TWELVE_HOURS, TWENTY_FOUR_HOURS)"
  type        = string
  default     = "TWELVE_HOURS"
  validation {
    condition     = contains(["ONE_HOUR", "SIX_HOURS", "TWELVE_HOURS", "TWENTY_FOUR_HOURS"], var.rdb_snapshot_period)
    error_message = "RDB snapshot period must be ONE_HOUR, SIX_HOURS, TWELVE_HOURS, or TWENTY_FOUR_HOURS."
  }
}

variable "prevent_destroy" {
  description = "Prevent Terraform from destroying this instance"
  type        = bool
  default     = false
}

variable "labels" {
  description = "Labels to apply to the Redis instance"
  type        = map(string)
  default     = {}
}
