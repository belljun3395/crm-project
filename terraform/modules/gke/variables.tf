variable "project_id" {
  description = "GCP project ID."
  type        = string
}

variable "region" {
  description = "Region where the GKE cluster will be created."
  type        = string
}

variable "zones" {
  description = "Zonal distribution for node pools."
  type        = list(string)
}

variable "cluster_name" {
  description = "GKE cluster name."
  type        = string
}

variable "network" {
  description = "VPC network name."
  type        = string
}

variable "subnetwork" {
  description = "Subnetwork name."
  type        = string
}

variable "master_ipv4_cidr_block" {
  description = "CIDR for the master nodes when using private clusters."
  type        = string
  default     = null
}

variable "cluster_secondary_range_pods" {
  description = "Name of the secondary IP range for pods."
  type        = string
}

variable "cluster_secondary_range_services" {
  description = "Name of the secondary IP range for services."
  type        = string
}

variable "release_channel" {
  description = "GKE release channel."
  type        = string
  default     = "REGULAR"
}

variable "min_master_version" {
  description = "Minimum master version. Optional when using release channels."
  type        = string
  default     = null
}

variable "enable_private_nodes" {
  description = "Enable private nodes."
  type        = bool
  default     = true
}

variable "enable_private_endpoint" {
  description = "Enable private endpoint."
  type        = bool
  default     = false
}

variable "master_authorized_networks" {
  description = "Authorized networks for master access."
  type = list(object({
    cidr_block   = string
    display_name = string
  }))
  default = []
}

variable "node_pool_machine_type" {
  description = "Machine type for node pool."
  type        = string
}

variable "node_pool_disk_size_gb" {
  description = "Disk size in GB."
  type        = number
}

variable "node_pool_min_count" {
  description = "Minimum node count."
  type        = number
}

variable "node_pool_max_count" {
  description = "Maximum node count."
  type        = number
}

variable "node_pool_initial_count" {
  description = "Initial node count."
  type        = number
}

variable "labels" {
  description = "Common labels for cluster resources."
  type        = map(string)
  default     = {}
}
