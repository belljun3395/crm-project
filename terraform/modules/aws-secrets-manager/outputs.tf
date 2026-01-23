output "secret_arn" {
  description = "ARN of the Secrets Manager secret."
  value       = aws_secretsmanager_secret.this.arn
}

output "secret_name" {
  description = "Name of the Secrets Manager secret."
  value       = aws_secretsmanager_secret.this.name
}

output "secret_version_id" {
  description = "Version ID of the stored secret string."
  value       = aws_secretsmanager_secret_version.this.version_id
}
