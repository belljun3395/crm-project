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
