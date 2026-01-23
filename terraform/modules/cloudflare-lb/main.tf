terraform {
  required_version = ">= 1.5.0"
  required_providers {
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = ">= 4.0"
    }
  }
}

# Health Check Monitor
resource "cloudflare_load_balancer_monitor" "health_check" {
  account_id = var.cloudflare_account_id

  type     = var.health_check_type
  port     = var.health_check_port
  method   = var.health_check_method
  path     = var.health_check_path
  interval = var.health_check_interval
  timeout  = var.health_check_timeout
  retries  = var.health_check_retries

  # Expected response
  expected_codes = var.health_check_expected_codes
  expected_body  = var.health_check_expected_body

  # Health check 설정
  follow_redirects = true
  allow_insecure   = false

  header {
    header = "Host"
    values = [var.domain_name]
  }

  description = "Health check for ${var.name_prefix}"
}

# AWS Pool
resource "cloudflare_load_balancer_pool" "aws_pool" {
  account_id = var.cloudflare_account_id
  name       = "${var.name_prefix}-aws-pool"

  # AWS origin
  dynamic "origins" {
    for_each = var.aws_origins
    content {
      name    = origins.value.name
      address = origins.value.address
      enabled = origins.value.enabled
      weight  = origins.value.weight
      header {
        header = "X-Cloud-Provider"
        values = ["AWS"]
      }
    }
  }

  # Health check
  monitor = cloudflare_load_balancer_monitor.health_check.id

  # Geographic routing (선택사항)
  latitude  = var.aws_pool_latitude
  longitude = var.aws_pool_longitude

  # Notification
  notification_email = var.notification_email

  check_regions = var.health_check_regions

  description = "AWS origin pool"
}

# GCP Pool
resource "cloudflare_load_balancer_pool" "gcp_pool" {
  account_id = var.cloudflare_account_id
  name       = "${var.name_prefix}-gcp-pool"

  # GCP origin
  dynamic "origins" {
    for_each = var.gcp_origins
    content {
      name    = origins.value.name
      address = origins.value.address
      enabled = origins.value.enabled
      weight  = origins.value.weight
      header {
        header = "X-Cloud-Provider"
        values = ["GCP"]
      }
    }
  }

  # Health check
  monitor = cloudflare_load_balancer_monitor.health_check.id

  # Geographic routing (선택사항)
  latitude  = var.gcp_pool_latitude
  longitude = var.gcp_pool_longitude

  # Notification
  notification_email = var.notification_email

  check_regions = var.health_check_regions

  description = "GCP origin pool"
}

# Global Load Balancer
resource "cloudflare_load_balancer" "main" {
  zone_id = var.cloudflare_zone_id
  name    = var.domain_name

  # Default pools (Active-Active)
  default_pool_ids = [
    cloudflare_load_balancer_pool.aws_pool.id,
    cloudflare_load_balancer_pool.gcp_pool.id,
  ]

  # Fallback pool (장애 시)
  fallback_pool_id = var.enable_failover ? (
    var.primary_cloud == "aws" ?
    cloudflare_load_balancer_pool.gcp_pool.id :
    cloudflare_load_balancer_pool.aws_pool.id
  ) : null

  # Steering Policy
  # - "off": Round robin (균등 분배)
  # - "geo": Geographic steering (지역 기반)
  # - "dynamic_latency": 지연시간 기반 (가장 빠른 곳으로)
  # - "random": Random
  steering_policy = var.steering_policy

  # Session Affinity (고정 세션)
  session_affinity     = var.session_affinity
  session_affinity_ttl = var.session_affinity_ttl

  # Session affinity 속성
  session_affinity_attributes {
    samesite       = "Lax"
    secure         = "Always"
    drain_duration = 0
  }

  # TTL
  ttl = 30

  # Enable/Disable
  enabled = true

  # Proxied (CloudFlare CDN 사용)
  proxied = var.proxied

  # Description
  description = "Active-Active load balancer for ${var.name_prefix}"

  # Region pools (지역별 라우팅)
  dynamic "region_pools" {
    for_each = var.enable_geo_routing ? var.region_pools : []
    content {
      region   = region_pools.value.region
      pool_ids = region_pools.value.pool_ids
    }
  }

  # Pop pools (특정 POP 라우팅)
  dynamic "pop_pools" {
    for_each = var.pop_pools
    content {
      pop      = pop_pools.value.pop
      pool_ids = pop_pools.value.pool_ids
    }
  }

  # Country pools (국가별 라우팅)
  dynamic "country_pools" {
    for_each = var.enable_geo_routing ? var.country_pools : []
    content {
      country  = country_pools.value.country
      pool_ids = country_pools.value.pool_ids
    }
  }

  # Rules (추가 라우팅 규칙)
  dynamic "rules" {
    for_each = var.lb_rules
    content {
      name      = rules.value.name
      condition = rules.value.condition
      disabled  = rules.value.disabled
      priority  = rules.value.priority

      dynamic "overrides" {
        for_each = rules.value.overrides != null ? [rules.value.overrides] : []
        content {
          session_affinity     = overrides.value.session_affinity
          session_affinity_ttl = overrides.value.session_affinity_ttl
          steering_policy      = overrides.value.steering_policy
          ttl                  = overrides.value.ttl
          fallback_pool        = overrides.value.fallback_pool
          default_pools        = overrides.value.default_pools
        }
      }
    }
  }

  # Adaptive routing (성능 기반 자동 최적화)
  adaptive_routing {
    failover_across_pools = true
  }

  # Location strategy (지역 전략)
  location_strategy {
    prefer_ecs = "proxied"
    mode       = var.location_strategy_mode
  }

  # Random steering (랜덤 가중치)
  dynamic "random_steering" {
    for_each = var.steering_policy == "random" ? [1] : []
    content {
      pool_weights = {
        (cloudflare_load_balancer_pool.aws_pool.id) = var.aws_pool_weight
        (cloudflare_load_balancer_pool.gcp_pool.id) = var.gcp_pool_weight
      }
      default_weight = 0.5
    }
  }
}

