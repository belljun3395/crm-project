locals {
  labels = var.labels
}

module "networking" {
  source = "../../modules/gcp-networking"

  project_id                     = var.project_id
  network_name                   = var.network_name
  region                         = var.region
  subnet_ip_cidr_range           = var.subnet_ip_cidr_range
  subnet_secondary_pods_cidr     = var.subnet_secondary_pods_cidr
  subnet_secondary_services_cidr = var.subnet_secondary_services_cidr
  ip_range_pods_name             = var.ip_range_pods_name
  ip_range_services_name         = var.ip_range_services_name
  labels                         = local.labels
}

module "gke" {
  source = "../../modules/gke"

  project_id                       = var.project_id
  region                           = var.region
  zones                            = var.zones
  cluster_name                     = var.cluster_name
  network                          = module.networking.network_name
  subnetwork                       = module.networking.subnet_name
  master_ipv4_cidr_block           = null
  cluster_secondary_range_pods     = var.ip_range_pods_name
  cluster_secondary_range_services = var.ip_range_services_name
  release_channel                  = var.cluster_release_channel
  min_master_version               = var.cluster_min_master_version
  enable_private_nodes             = var.cluster_enable_private_nodes
  enable_private_endpoint          = var.cluster_enable_private_endpoint
  master_authorized_networks       = var.master_authorized_networks
  node_pool_machine_type           = var.node_pool_machine_type
  node_pool_disk_size_gb           = var.node_pool_disk_size_gb
  node_pool_min_count              = var.node_pool_min_count
  node_pool_max_count              = var.node_pool_max_count
  node_pool_initial_count          = var.node_pool_initial_count
  labels                           = local.labels

  depends_on = [module.networking]
}

module "artifact_registry" {
  source = "../../modules/artifact_registry"

  project_id    = var.project_id
  location      = var.artifact_registry_location
  repository_id = var.artifact_registry_repository_id
  format        = var.artifact_registry_format
  labels        = local.labels
}
