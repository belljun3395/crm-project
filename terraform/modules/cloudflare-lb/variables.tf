variable "name_prefix" {
  description = "Prefix for resource names"
  type        = string
}

variable "cloudflare_account_id" {
  description = "CloudFlare account ID"
  type        = string
}

variable "cloudflare_zone_id" {
  description = "CloudFlare zone ID"
  type        = string
}

variable "domain_name" {
  description = "Domain name for the load balancer"
  type        = string
}

variable "dns_record_name" {
  description = "DNS record name (subdomain)"
  type        = string
  default     = "@"
}

# Health Check Configuration
variable "health_check_type" {
  description = "Health check type (http, https, tcp)"
  type        = string
  default     = "https"
}

variable "health_check_port" {
  description = "Port for health checks"
  type        = number
  default     = 443
}

variable "health_check_method" {
  description = "HTTP method for health checks"
  type        = string
  default     = "GET"
}

variable "health_check_path" {
  description = "Path for health checks"
  type        = string
  default     = "/health"
}

variable "health_check_interval" {
  description = "Interval between health checks (seconds)"
  type        = number
  default     = 60
}

variable "health_check_timeout" {
  description = "Timeout for health checks (seconds)"
  type        = number
  default     = 5
}

variable "health_check_retries" {
  description = "Number of retries for health checks"
  type        = number
  default     = 2
}

variable "health_check_expected_codes" {
  description = "Expected HTTP status codes"
  type        = string
  default     = "200"
}

variable "health_check_expected_body" {
  description = "Expected response body (optional)"
  type        = string
  default     = ""
}

variable "health_check_regions" {
  description = "Regions to perform health checks from"
  type        = list(string)
  default     = ["WNAM", "ENAM", "WEU", "EEU", "SEAS", "NEAS"]
}

# AWS Pool Configuration
variable "aws_origins" {
  description = "AWS origin servers"
  type = list(object({
    name    = string
    address = string
    enabled = bool
    weight  = number
  }))
}

variable "aws_pool_latitude" {
  description = "AWS pool latitude for geographic routing"
  type        = number
  default     = null
}

variable "aws_pool_longitude" {
  description = "AWS pool longitude for geographic routing"
  type        = number
  default     = null
}

variable "aws_pool_weight" {
  description = "Weight for AWS pool in random steering"
  type        = number
  default     = 0.5
}

# GCP Pool Configuration
variable "gcp_origins" {
  description = "GCP origin servers"
  type = list(object({
    name    = string
    address = string
    enabled = bool
    weight  = number
  }))
}

variable "gcp_pool_latitude" {
  description = "GCP pool latitude for geographic routing"
  type        = number
  default     = null
}

variable "gcp_pool_longitude" {
  description = "GCP pool longitude for geographic routing"
  type        = number
  default     = null
}

variable "gcp_pool_weight" {
  description = "Weight for GCP pool in random steering"
  type        = number
  default     = 0.5
}

# Load Balancer Configuration
variable "steering_policy" {
  description = "Steering policy (off, geo, dynamic_latency, random)"
  type        = string
  default     = "dynamic_latency"
  validation {
    condition     = contains(["off", "geo", "dynamic_latency", "random", "proximity", "least_outstanding_requests", "least_connections"], var.steering_policy)
    error_message = "Invalid steering policy"
  }
}

variable "session_affinity" {
  description = "Session affinity (none, cookie, ip_cookie)"
  type        = string
  default     = "cookie"
  validation {
    condition     = contains(["none", "cookie", "ip_cookie", "header"], var.session_affinity)
    error_message = "Invalid session affinity"
  }
}

variable "session_affinity_ttl" {
  description = "Session affinity TTL (seconds)"
  type        = number
  default     = 3600
}

variable "proxied" {
  description = "Enable CloudFlare proxy (CDN)"
  type        = bool
  default     = true
}

variable "enable_failover" {
  description = "Enable automatic failover"
  type        = bool
  default     = true
}

variable "primary_cloud" {
  description = "Primary cloud provider (aws or gcp)"
  type        = string
  default     = "aws"
  validation {
    condition     = contains(["aws", "gcp"], var.primary_cloud)
    error_message = "Primary cloud must be 'aws' or 'gcp'"
  }
}

# Geographic Routing
variable "enable_geo_routing" {
  description = "Enable geographic routing"
  type        = bool
  default     = false
}

variable "region_pools" {
  description = "Region-specific pool routing"
  type = list(object({
    region   = string
    pool_ids = list(string)
  }))
  default = []
}

variable "country_pools" {
  description = "Country-specific pool routing"
  type = list(object({
    country  = string
    pool_ids = list(string)
  }))
  default = []
}

variable "pop_pools" {
  description = "POP-specific pool routing"
  type = list(object({
    pop      = string
    pool_ids = list(string)
  }))
  default = []
}

# Advanced Configuration
variable "lb_rules" {
  description = "Load balancer rules"
  type = list(object({
    name      = string
    condition = string
    disabled  = bool
    priority  = number
    overrides = object({
      session_affinity     = string
      session_affinity_ttl = number
      steering_policy      = string
      ttl                  = number
      fallback_pool        = string
      default_pools        = list(string)
    })
  }))
  default = []
}

variable "location_strategy_mode" {
  description = "Location strategy mode (pop, resolver_ip)"
  type        = string
  default     = "resolver_ip"
}

variable "notification_email" {
  description = "Email for health check notifications"
  type        = string
  default     = ""
}
