output "id" {
  description = "Identifier of the Redis instance"
  value       = google_redis_instance.this.id
}

output "name" {
  description = "Name of the Redis instance"
  value       = google_redis_instance.this.name
}

output "host" {
  description = "Hostname or IP address of the Redis instance"
  value       = google_redis_instance.this.host
}

output "port" {
  description = "Port number of the Redis instance"
  value       = google_redis_instance.this.port
}

output "current_location_id" {
  description = "Current zone where the Redis endpoint is placed"
  value       = google_redis_instance.this.current_location_id
}

output "persistence_iam_identity" {
  description = "Cloud IAM identity used by import/export operations"
  value       = google_redis_instance.this.persistence_iam_identity
}

output "server_ca_certs" {
  description = "List of server CA certificates for TLS connections"
  value       = google_redis_instance.this.server_ca_certs
  sensitive   = true
}

output "auth_string" {
  description = "AUTH string for Redis instance (if auth_enabled is true)"
  value       = google_redis_instance.this.auth_string
  sensitive   = true
}

output "connection_string" {
  description = "Redis connection string (host:port)"
  value       = "${google_redis_instance.this.host}:${google_redis_instance.this.port}"
}

output "read_endpoint" {
  description = "Read endpoint for the instance (for read replicas)"
  value       = google_redis_instance.this.read_endpoint
}

output "read_endpoint_port" {
  description = "Read endpoint port"
  value       = google_redis_instance.this.read_endpoint_port
}
