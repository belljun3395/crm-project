output "network_name" {
  description = "Name of the created VPC network."
  value       = google_compute_network.this.name
}

output "network_self_link" {
  description = "Self link of the VPC network."
  value       = google_compute_network.this.self_link
}

output "subnet_name" {
  description = "Name of the primary subnet."
  value       = google_compute_subnetwork.primary.name
}

output "subnet_self_link" {
  description = "Self link of the primary subnet."
  value       = google_compute_subnetwork.primary.self_link
}
