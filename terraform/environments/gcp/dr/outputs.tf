output "network_name" {
  value       = module.networking.network_name
  description = "Provisioned VPC network name."
}

output "gke_cluster_endpoint" {
  value       = module.gke.cluster_endpoint
  description = "Endpoint URL for the GKE control plane."
}

output "gke_cluster_ca_certificate" {
  value       = module.gke.cluster_ca_certificate
  description = "CA certificate for authenticating to the cluster."
  sensitive   = true
}

output "artifact_registry_repository_id" {
  value       = module.artifact_registry.repository_id
  description = "Artifact Registry repository identifier."
}

# Secret Manager Outputs
output "secret_manager_secret_id" {
  value       = var.enable_secret_manager ? module.secret_manager[0].secret_id : null
  description = "Secret Manager secret ID"
}

output "secret_manager_secret_name" {
  value       = var.enable_secret_manager ? module.secret_manager[0].name : null
  description = "Secret Manager secret name"
}

# Cloud SQL Outputs
output "cloud_sql_instance_name" {
  value       = var.enable_cloud_sql ? module.cloud_sql[0].instance_name : null
  description = "Cloud SQL instance name"
}

output "cloud_sql_connection_name" {
  value       = var.enable_cloud_sql ? module.cloud_sql[0].connection_name : null
  description = "Cloud SQL connection name"
}

output "cloud_sql_private_ip_address" {
  value       = var.enable_cloud_sql ? module.cloud_sql[0].private_ip_address : null
  description = "Cloud SQL private IP address"
}

output "cloud_sql_public_ip_address" {
  value       = var.enable_cloud_sql ? module.cloud_sql[0].public_ip_address : null
  description = "Cloud SQL public IP address"
}

# Memorystore Outputs
output "memorystore_instance_id" {
  value       = var.enable_memorystore ? module.memorystore[0].id : null
  description = "Memorystore instance ID"
}

output "memorystore_host" {
  value       = var.enable_memorystore ? module.memorystore[0].host : null
  description = "Memorystore host address"
}

output "memorystore_port" {
  value       = var.enable_memorystore ? module.memorystore[0].port : null
  description = "Memorystore port"
}

output "memorystore_connection_string" {
  value       = var.enable_memorystore ? module.memorystore[0].connection_string : null
  description = "Memorystore connection string"
}

# Kafka Outputs
output "kafka_bootstrap_servers" {
  value       = module.kafka.kafka_bootstrap_servers
  description = "Kafka bootstrap servers (internal)"
}
