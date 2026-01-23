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

# Secret Manager
module "secret_manager" {
  source = "../../modules/gcp-secret-manager"

  count = var.enable_secret_manager ? 1 : 0

  project_id  = var.project_id
  secret_id   = var.secret_manager_secret_id
  secret_data = var.secret_manager_secret_data
  replication = var.secret_manager_replication
  labels      = local.labels
}

# Database (Cloud SQL - PostgreSQL)
module "cloud_sql" {
  source = "../../modules/cloud-sql"

  count = var.enable_cloud_sql ? 1 : 0

  project_id                     = var.project_id
  name                           = var.cloud_sql_instance_name
  region                         = var.region
  database_version               = var.cloud_sql_database_version
  tier                           = var.cloud_sql_tier
  disk_size                      = var.cloud_sql_disk_size
  disk_type                      = var.cloud_sql_disk_type
  availability_type              = var.cloud_sql_availability_type
  database_name                  = var.cloud_sql_database_name
  user_name                      = var.cloud_sql_user_name
  user_password                  = var.cloud_sql_user_password
  authorized_networks            = var.cloud_sql_authorized_networks
  backup_enabled                 = var.cloud_sql_backup_enabled
  backup_start_time              = var.cloud_sql_backup_start_time
  point_in_time_recovery_enabled = var.cloud_sql_point_in_time_recovery_enabled
  deletion_protection            = var.cloud_sql_deletion_protection
  labels                         = local.labels

  depends_on = [module.networking]
}

# Cache (Memorystore - Redis)
module "memorystore" {
  source = "../../modules/memorystore"

  count = var.enable_memorystore ? 1 : 0

  instance_id             = var.memorystore_instance_id
  display_name            = var.memorystore_display_name
  region                  = var.region
  tier                    = var.memorystore_tier
  memory_size_gb          = var.memorystore_memory_size_gb
  redis_version           = var.memorystore_redis_version
  replica_count           = var.memorystore_replica_count
  authorized_network      = module.networking.network_id
  auth_enabled            = var.memorystore_auth_enabled
  transit_encryption_mode = var.memorystore_transit_encryption_mode
  persistence_mode        = var.memorystore_persistence_mode
  rdb_snapshot_period     = var.memorystore_rdb_snapshot_period
  labels                  = local.labels

  depends_on = [module.networking]
}

# Kafka (Strimzi on GKE)
module "kafka" {
  source = "../../modules/gke-kafka"

  cluster_name  = "gcp-kafka"
  namespace     = "kafka"
  kafka_version = "3.6.0"
  replicas      = 3

  depends_on = [module.gke]
}
