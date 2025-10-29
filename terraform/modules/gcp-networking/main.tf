terraform {
  required_version = ">= 1.5.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 5.0"
    }
  }
}

resource "google_compute_network" "this" {
  name                    = var.network_name
  project                 = var.project_id
  auto_create_subnetworks = false

  routing_mode = "REGIONAL"

  delete_default_routes_on_create = false

  timeouts {
    create = "10m"
    delete = "10m"
  }
}

resource "google_compute_subnetwork" "primary" {
  name          = "${var.network_name}-subnet"
  project       = var.project_id
  region        = var.region
  ip_cidr_range = var.subnet_ip_cidr_range
  network       = google_compute_network.this.id

  private_ip_google_access = true

  secondary_ip_range {
    range_name    = var.ip_range_pods_name
    ip_cidr_range = var.subnet_secondary_pods_cidr
  }

  secondary_ip_range {
    range_name    = var.ip_range_services_name
    ip_cidr_range = var.subnet_secondary_services_cidr
  }

  stack_type = "IPV4"

  timeouts {
    create = "10m"
    delete = "10m"
  }

  dynamic "log_config" {
    for_each = var.labels["enable_flow_logs"] == "true" ? [1] : []
    content {
      aggregation_interval = "INTERVAL_5_SEC"
      flow_sampling        = 0.5
      metadata             = "INCLUDE_ALL_METADATA"
    }
  }

  depends_on = [google_compute_network.this]
}

resource "google_compute_router" "nat" {
  name    = "${var.network_name}-router"
  project = var.project_id
  region  = var.region
  network = google_compute_network.this.id
}

resource "google_compute_router_nat" "nat" {
  name                                = "${var.network_name}-nat"
  project                             = var.project_id
  region                              = var.region
  router                              = google_compute_router.nat.name
  nat_ip_allocate_option              = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat  = "ALL_SUBNETWORKS_ALL_IP_RANGES"
  enable_endpoint_independent_mapping = true

  log_config {
    enable = true
    filter = "ERRORS_ONLY"
  }
}
