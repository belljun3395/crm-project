variable "instance_name" {
  description = "Base name for the Cloud SQL instance (suffix will be added)"
  type        = string
}

variable "database_version" {
  description = "Database version (e.g., POSTGRES_15)"
  type        = string
  default     = "POSTGRES_15"
}

variable "region" {
  description = "GCP region for the instance"
  type        = string
}

variable "tier" {
  description = "Machine type tier"
  type        = string
  default     = "db-f1-micro"
}

variable "availability_type" {
  description = "Availability type (ZONAL or REGIONAL for high availability)"
  type        = string
  default     = "ZONAL"
}

variable "disk_type" {
  description = "Disk type (PD_SSD or PD_HDD)"
  type        = string
  default     = "PD_SSD"
}

variable "disk_size" {
  description = "Disk size in GB"
  type        = number
  default     = 10
}

variable "disk_autoresize" {
  description = "Enable automatic disk size increase"
  type        = bool
  default     = true
}

variable "disk_autoresize_limit" {
  description = "Maximum disk size for autoresize in GB"
  type        = number
  default     = 100
}

variable "database_name" {
  description = "Name of the database to create"
  type        = string
}

variable "db_user" {
  description = "Database user name"
  type        = string
}

variable "db_password" {
  description = "Database user password"
  type        = string
  sensitive   = true
}

variable "backup_enabled" {
  description = "Enable automated backups"
  type        = bool
  default     = true
}

variable "backup_start_time" {
  description = "Backup start time in HH:MM format"
  type        = string
  default     = "03:00"
}

variable "point_in_time_recovery_enabled" {
  description = "Enable point-in-time recovery"
  type        = bool
  default     = true
}

variable "transaction_log_retention_days" {
  description = "Number of days to retain transaction logs"
  type        = number
  default     = 7
}

variable "backup_retention_count" {
  description = "Number of backups to retain"
  type        = number
  default     = 7
}

variable "ipv4_enabled" {
  description = "Enable public IP address"
  type        = bool
  default     = false
}

variable "private_network" {
  description = "VPC network for private IP"
  type        = string
  default     = null
}

variable "require_ssl" {
  description = "Require SSL for connections"
  type        = bool
  default     = true
}

variable "authorized_networks" {
  description = "List of authorized networks for public IP access"
  type = list(object({
    name  = string
    value = string
  }))
  default = []
}

variable "maintenance_window_day" {
  description = "Day of week for maintenance (1-7, 1 = Monday)"
  type        = number
  default     = 7
}

variable "maintenance_window_hour" {
  description = "Hour of day for maintenance (0-23)"
  type        = number
  default     = 4
}

variable "maintenance_window_update_track" {
  description = "Maintenance timing (stable or canary)"
  type        = string
  default     = "stable"
}

variable "database_flags" {
  description = "List of database flags"
  type = list(object({
    name  = string
    value = string
  }))
  default = []
}

variable "query_insights_enabled" {
  description = "Enable query insights"
  type        = bool
  default     = true
}

variable "query_string_length" {
  description = "Maximum query string length for insights"
  type        = number
  default     = 1024
}

variable "record_application_tags" {
  description = "Record application tags in query insights"
  type        = bool
  default     = true
}

variable "record_client_address" {
  description = "Record client address in query insights"
  type        = bool
  default     = true
}

variable "deletion_protection" {
  description = "Enable deletion protection"
  type        = bool
  default     = true
}
