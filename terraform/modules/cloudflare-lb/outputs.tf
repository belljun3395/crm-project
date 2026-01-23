output "load_balancer_id" {
  description = "Load balancer ID"
  value       = cloudflare_load_balancer.main.id
}

output "load_balancer_name" {
  description = "Load balancer name"
  value       = cloudflare_load_balancer.main.name
}

output "aws_pool_id" {
  description = "AWS pool ID"
  value       = cloudflare_load_balancer_pool.aws_pool.id
}

output "gcp_pool_id" {
  description = "GCP pool ID"
  value       = cloudflare_load_balancer_pool.gcp_pool.id
}

output "health_check_id" {
  description = "Health check monitor ID"
  value       = cloudflare_load_balancer_monitor.health_check.id
}

output "dns_record_name" {
  description = "DNS record full name"
  value       = cloudflare_record.lb.hostname
}
