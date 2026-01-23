variable "project_id" {
  description = "GCP project ID where resources will be provisioned."
  type        = string
}

variable "region" {
  description = "Default region for regional resources."
  type        = string
  default     = "asia-northeast3"
}

variable "zones" {
  description = "List of zones used for zonal resources such as GKE node pools."
  type        = list(string)
  default     = ["asia-northeast3-a", "asia-northeast3-b"]
}

variable "network_name" {
  description = "Name of the VPC network."
  type        = string
  default     = "crm-dev"
}

variable "subnet_ip_cidr_range" {
  description = "Primary CIDR block for the GKE subnet."
  type        = string
  default     = "10.20.0.0/20"
}

variable "subnet_secondary_pods_cidr" {
  description = "Secondary CIDR for GKE pods."
  type        = string
  default     = "10.21.0.0/16"
}

variable "subnet_secondary_services_cidr" {
  description = "Secondary CIDR for GKE services."
  type        = string
  default     = "10.22.0.0/20"
}

variable "ip_range_pods_name" {
  description = "Name of the secondary range for pods."
  type        = string
  default     = "crm-dev-pods"
}

variable "ip_range_services_name" {
  description = "Name of the secondary range for services."
  type        = string
  default     = "crm-dev-services"
}

variable "cluster_name" {
  description = "Name of the GKE cluster."
  type        = string
  default     = "crm-dev-gke"
}

variable "cluster_release_channel" {
  description = "GKE release channel (RAPID, REGULAR, STABLE)."
  type        = string
  default     = "REGULAR"
}

variable "cluster_min_master_version" {
  description = "Minimum master version. Optional because release channel can manage it."
  type        = string
  default     = null
}

variable "cluster_enable_private_nodes" {
  description = "Whether to create private GKE nodes."
  type        = bool
  default     = true
}

variable "cluster_enable_private_endpoint" {
  description = "Whether to expose the master endpoint privately."
  type        = bool
  default     = false
}

variable "master_authorized_networks" {
  description = "CIDR blocks allowed to reach the GKE control plane."
  type = list(object({
    cidr_block   = string
    display_name = string
  }))
  default = [
    {
      cidr_block   = "0.0.0.0/0"
      display_name = "public"
    }
  ]
}

variable "node_pool_machine_type" {
  description = "Machine type for the default node pool."
  type        = string
  default     = "e2-standard-4"
}

variable "node_pool_disk_size_gb" {
  description = "Disk size for the nodes in GB."
  type        = number
  default     = 100
}

variable "node_pool_min_count" {
  description = "Minimum node count."
  type        = number
  default     = 1
}

variable "node_pool_max_count" {
  description = "Maximum node count."
  type        = number
  default     = 3
}

variable "node_pool_initial_count" {
  description = "Initial node count."
  type        = number
  default     = 1
}

variable "artifact_registry_location" {
  description = "Region for the Artifact Registry repository."
  type        = string
  default     = "asia-northeast3"
}

variable "artifact_registry_repository_id" {
  description = "ID of the Artifact Registry repository."
  type        = string
  default     = "crm-app"
}

variable "artifact_registry_format" {
  description = "Repository format. e.g., DOCKER."
  type        = string
  default     = "DOCKER"
}

variable "labels" {
  description = "Common labels applied across resources."
  type        = map(string)
  default = {
    project     = "crm"
    environment = "dev"
    managed_by  = "terraform"
  }
}

# Secret Manager Variables
variable "enable_secret_manager" {
  description = "Enable GCP Secret Manager"
  type        = bool
  default     = false
}

variable "secret_manager_secret_id" {
  description = "Secret ID for Secret Manager"
  type        = string
  default     = "crm-dev-application"
}

variable "secret_manager_secret_data" {
  description = "Secret data (JSON string)"
  type        = string
  sensitive   = true
  default     = "{}"
}

variable "secret_manager_replication" {
  description = "Replication policy (automatic or user_managed)"
  type        = string
  default     = "automatic"
}

# Cloud SQL Variables
variable "enable_cloud_sql" {
  description = "Enable Cloud SQL PostgreSQL database"
  type        = bool
  default     = false
}

variable "cloud_sql_instance_name" {
  description = "Cloud SQL instance name"
  type        = string
  default     = "crm-dev-postgres"
}

variable "cloud_sql_database_version" {
  description = "PostgreSQL version"
  type        = string
  default     = "POSTGRES_15"
}

variable "cloud_sql_tier" {
  description = "Machine tier for Cloud SQL"
  type        = string
  default     = "db-f1-micro"
}

variable "cloud_sql_disk_size" {
  description = "Disk size in GB"
  type        = number
  default     = 10
}

variable "cloud_sql_disk_type" {
  description = "Disk type (PD_SSD or PD_HDD)"
  type        = string
  default     = "PD_SSD"
}

variable "cloud_sql_availability_type" {
  description = "Availability type (REGIONAL or ZONAL)"
  type        = string
  default     = "ZONAL"
}

variable "cloud_sql_database_name" {
  description = "Database name"
  type        = string
  default     = "crm"
}

variable "cloud_sql_user_name" {
  description = "Database user name"
  type        = string
  default     = "crmadmin"
}

variable "cloud_sql_user_password" {
  description = "Database user password"
  type        = string
  sensitive   = true
  default     = null
}

variable "cloud_sql_authorized_networks" {
  description = "Authorized networks for Cloud SQL"
  type = list(object({
    name  = string
    value = string
  }))
  default = []
}

variable "cloud_sql_backup_enabled" {
  description = "Enable automated backups"
  type        = bool
  default     = true
}

variable "cloud_sql_backup_start_time" {
  description = "Backup start time (HH:MM format)"
  type        = string
  default     = "03:00"
}

variable "cloud_sql_point_in_time_recovery_enabled" {
  description = "Enable point-in-time recovery"
  type        = bool
  default     = true
}

variable "cloud_sql_deletion_protection" {
  description = "Enable deletion protection"
  type        = bool
  default     = false
}

# Memorystore Variables
variable "enable_memorystore" {
  description = "Enable Memorystore Redis instance"
  type        = bool
  default     = false
}

variable "memorystore_instance_id" {
  description = "Memorystore instance ID"
  type        = string
  default     = "crm-dev-redis"
}

variable "memorystore_display_name" {
  description = "Display name for Memorystore instance"
  type        = string
  default     = "CRM Dev Redis"
}

variable "memorystore_tier" {
  description = "Service tier (BASIC or STANDARD_HA)"
  type        = string
  default     = "BASIC"
}

variable "memorystore_memory_size_gb" {
  description = "Memory size in GB"
  type        = number
  default     = 1
}

variable "memorystore_redis_version" {
  description = "Redis version"
  type        = string
  default     = "REDIS_7_0"
}

variable "memorystore_replica_count" {
  description = "Number of replicas"
  type        = number
  default     = 0
}

variable "memorystore_auth_enabled" {
  description = "Enable Redis AUTH"
  type        = bool
  default     = true
}

variable "memorystore_transit_encryption_mode" {
  description = "Transit encryption mode"
  type        = string
  default     = "SERVER_AUTHENTICATION"
}

variable "memorystore_persistence_mode" {
  description = "Persistence mode (DISABLED or RDB)"
  type        = string
  default     = "DISABLED"
}

variable "memorystore_rdb_snapshot_period" {
  description = "RDB snapshot period"
  type        = string
  default     = "TWELVE_HOURS"
}
