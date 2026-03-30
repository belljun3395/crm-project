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
  value       = try(module.rds[0].db_instance_endpoint, null)
}

output "rds_address" {
  description = "RDS instance address"
  value       = try(module.rds[0].db_instance_address, null)
}

output "rds_port" {
  description = "RDS instance port"
  value       = try(module.rds[0].db_instance_port, null)
}

output "rds_database_name" {
  description = "RDS database name"
  value       = try(module.rds[0].db_instance_name, null)
}

# ElastiCache Outputs
output "elasticache_primary_endpoint" {
  description = "ElastiCache primary endpoint"
  value       = try(module.elasticache[0].primary_endpoint_address, null)
}

output "elasticache_reader_endpoint" {
  description = "ElastiCache reader endpoint"
  value       = try(module.elasticache[0].reader_endpoint_address, null)
}

output "elasticache_configuration_endpoint" {
  description = "ElastiCache configuration endpoint"
  value       = try(module.elasticache[0].configuration_endpoint_address, null)
}

output "elasticache_port" {
  description = "ElastiCache port"
  value       = try(module.elasticache[0].port, null)
}

output "app_runtime_cache_invalidation_topic_arn" {
  description = "SNS topic ARN used for cache invalidation"
  value       = module.app_runtime.cache_invalidation_topic_arn
}

output "app_runtime_schedule_queue_arn" {
  description = "SQS queue ARN used by EventBridge Scheduler"
  value       = module.app_runtime.schedule_queue_arn
}

output "app_runtime_schedule_role_arn" {
  description = "IAM role ARN used by EventBridge Scheduler"
  value       = module.app_runtime.schedule_role_arn
}

output "app_runtime_schedule_group_name" {
  description = "EventBridge Scheduler group name"
  value       = module.app_runtime.schedule_group_name
}

output "app_runtime_ses_configuration_set_name" {
  description = "SES configuration set name"
  value       = module.app_runtime.ses_configuration_set_name
}

output "app_runtime_ses_queue_arn" {
  description = "SQS queue ARN used for SES notifications"
  value       = module.app_runtime.ses_queue_arn
}
