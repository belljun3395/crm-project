output "vpc_id" {
  description = "ID of the provisioned VPC."
  value       = module.networking.vpc_id
}

output "public_subnet_ids" {
  description = "Public subnet IDs."
  value       = module.networking.public_subnet_ids
}

output "private_subnet_ids" {
  description = "Private subnet IDs."
  value       = module.networking.private_subnet_ids
}

output "cluster_endpoint" {
  description = "EKS control plane endpoint."
  value       = module.eks.cluster_endpoint
}

output "cluster_certificate_authority_data" {
  description = "CA bundle for configuring kubectl."
  value       = module.eks.cluster_certificate_authority_data
  sensitive   = true
}

output "ecr_repository_url" {
  description = "URI for pushing images."
  value       = module.ecr.repository_url
}

output "app_secret_arn" {
  description = "ARN of the Secrets Manager secret containing application configuration."
  value       = module.app_secret.secret_arn
}

output "app_secret_name" {
  description = "Name of the Secrets Manager secret containing application configuration."
  value       = module.app_secret.secret_name
}

# RDS Outputs
output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = var.enable_rds ? module.rds[0].endpoint : null
}

output "rds_address" {
  description = "RDS instance address"
  value       = var.enable_rds ? module.rds[0].address : null
}

output "rds_port" {
  description = "RDS instance port"
  value       = var.enable_rds ? module.rds[0].port : null
}

output "rds_database_name" {
  description = "RDS database name"
  value       = var.enable_rds ? module.rds[0].database_name : null
}

# ElastiCache Outputs
output "elasticache_primary_endpoint" {
  description = "ElastiCache primary endpoint"
  value       = var.enable_elasticache ? module.elasticache[0].primary_endpoint_address : null
}

output "elasticache_reader_endpoint" {
  description = "ElastiCache reader endpoint"
  value       = var.enable_elasticache ? module.elasticache[0].reader_endpoint_address : null
}

output "elasticache_port" {
  description = "ElastiCache port"
  value       = var.enable_elasticache ? module.elasticache[0].port : null
}
