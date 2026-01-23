output "instance_name" {
  description = "Name of the Cloud SQL instance"
  value       = google_sql_database_instance.this.name
}

output "instance_connection_name" {
  description = "Connection name for the instance (project:region:instance)"
  value       = google_sql_database_instance.this.connection_name
}

output "instance_self_link" {
  description = "Self link of the Cloud SQL instance"
  value       = google_sql_database_instance.this.self_link
}

output "private_ip_address" {
  description = "Private IP address of the instance"
  value       = length(google_sql_database_instance.this.private_ip_address) > 0 ? google_sql_database_instance.this.private_ip_address : null
}

output "public_ip_address" {
  description = "Public IP address of the instance"
  value       = length(google_sql_database_instance.this.public_ip_address) > 0 ? google_sql_database_instance.this.public_ip_address : null
}

output "database_name" {
  description = "Name of the created database"
  value       = google_sql_database.this.name
}

output "db_user" {
  description = "Database user name"
  value       = google_sql_user.this.name
  sensitive   = true
}
