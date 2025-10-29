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
