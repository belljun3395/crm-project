output "replication_group_id" {
  description = "ID of the ElastiCache replication group"
  value       = aws_elasticache_replication_group.this.id
}

output "replication_group_arn" {
  description = "ARN of the ElastiCache replication group"
  value       = aws_elasticache_replication_group.this.arn
}

output "primary_endpoint_address" {
  description = "Primary endpoint address"
  value       = aws_elasticache_replication_group.this.primary_endpoint_address
}

output "reader_endpoint_address" {
  description = "Reader endpoint address"
  value       = aws_elasticache_replication_group.this.reader_endpoint_address
}

output "configuration_endpoint_address" {
  description = "Configuration endpoint address (cluster mode only)"
  value       = aws_elasticache_replication_group.this.configuration_endpoint_address
}

output "port" {
  description = "Redis port"
  value       = aws_elasticache_replication_group.this.port
}

output "member_clusters" {
  description = "List of member cluster IDs"
  value       = aws_elasticache_replication_group.this.member_clusters
}

output "subnet_group_name" {
  description = "Name of the subnet group"
  value       = aws_elasticache_subnet_group.this.name
}

output "security_group_id" {
  description = "ID of the security group"
  value       = aws_security_group.this.id
}
