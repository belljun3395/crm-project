output "secret_id" {
  description = "ID of the Secret Manager secret."
  value       = google_secret_manager_secret.this.id
}

output "secret_name" {
  description = "Name of the Secret Manager secret."
  value       = google_secret_manager_secret.this.secret_id
}

output "version_name" {
  description = "Version name of the stored secret string."
  value       = google_secret_manager_secret_version.this.name
}
