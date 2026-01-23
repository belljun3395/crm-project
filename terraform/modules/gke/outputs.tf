output "cluster_name" {
  description = "Name of the created GKE cluster."
  value       = google_container_cluster.this.name
}

output "cluster_endpoint" {
  description = "Endpoint of the GKE control plane."
  value       = google_container_cluster.this.endpoint
}

output "cluster_ca_certificate" {
  description = "Certificate authority data for kubeconfig."
  value       = google_container_cluster.this.master_auth[0].cluster_ca_certificate
  sensitive   = true
}

output "node_service_account_email" {
  description = "Service account email used by the node pool."
  value       = google_service_account.node_pool.email
}
