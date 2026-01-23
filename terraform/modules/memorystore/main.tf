terraform {
  required_version = ">= 1.5.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 5.0"
    }
  }
}

# Redis Instance
resource "google_redis_instance" "this" {
  name               = var.instance_id
  display_name       = var.display_name
  tier               = var.tier
  memory_size_gb     = var.memory_size_gb
  region             = var.region
  redis_version      = var.redis_version
  replica_count      = var.replica_count
  read_replicas_mode = var.read_replicas_mode

  # Network
  authorized_network = var.authorized_network
  connect_mode       = var.connect_mode
  reserved_ip_range  = var.reserved_ip_range

  # Auth
  auth_enabled            = var.auth_enabled
  transit_encryption_mode = var.transit_encryption_mode

  # Configuration
  redis_configs = var.redis_configs

  # Maintenance
  maintenance_policy {
    weekly_maintenance_window {
      day = var.maintenance_day
      start_time {
        hours   = var.maintenance_start_hour
        minutes = var.maintenance_start_minute
        seconds = 0
        nanos   = 0
      }
    }
  }

  # Persistence
  persistence_config {
    persistence_mode    = var.persistence_mode
    rdb_snapshot_period = var.rdb_snapshot_period
  }

  # Labels
  labels = var.labels

  lifecycle {
    prevent_destroy = var.prevent_destroy
    ignore_changes = [
      maintenance_policy[0].weekly_maintenance_window[0].start_time[0].nanos,
    ]
  }
}
