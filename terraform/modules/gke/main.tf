terraform {
  required_version = ">= 1.5.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 5.0"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = ">= 5.0"
    }
  }
}

locals {
  cluster_labels = var.labels
}

resource "google_service_account" "node_pool" {
  project      = var.project_id
  account_id   = "${var.cluster_name}-nodes"
  display_name = "${var.cluster_name} GKE nodes"
}

resource "google_project_iam_member" "node_roles" {
  for_each = toset([
    "roles/logging.logWriter",
    "roles/monitoring.metricWriter",
    "roles/monitoring.viewer"
  ])

  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${google_service_account.node_pool.email}"
}

resource "google_container_cluster" "this" {
  provider = google-beta

  name                     = var.cluster_name
  project                  = var.project_id
  location                 = var.region
  network                  = var.network
  subnetwork               = var.subnetwork
  remove_default_node_pool = true
  initial_node_count       = 1

  release_channel {
    channel = var.release_channel
  }

  min_master_version = var.min_master_version

  logging_config {
    enable_components = ["SYSTEM_COMPONENTS", "WORKLOADS"]
  }

  monitoring_config {
    enable_components = ["SYSTEM_COMPONENTS", "APISERVER"]
    managed_prometheus {
      enabled = true
    }
  }

  workload_identity_config {
    workload_pool = "${var.project_id}.svc.id.goog"
  }

  ip_allocation_policy {
    cluster_secondary_range_name  = var.cluster_secondary_range_pods
    services_secondary_range_name = var.cluster_secondary_range_services
  }

  datapath_provider = "ADVANCED_DATAPATH"

  private_cluster_config {
    enable_private_nodes    = var.enable_private_nodes
    enable_private_endpoint = var.enable_private_endpoint
    master_ipv4_cidr_block  = var.master_ipv4_cidr_block
  }

  master_authorized_networks_config {
    dynamic "cidr_blocks" {
      for_each = var.master_authorized_networks
      content {
        cidr_block   = cidr_blocks.value.cidr_block
        display_name = cidr_blocks.value.display_name
      }
    }
  }

  vertical_pod_autoscaling {
    enabled = true
  }

  cluster_autoscaling {
    enabled = true
    auto_provisioning_defaults {
      oauth_scopes = [
        "https://www.googleapis.com/auth/cloud-platform"
      ]
    }
  }

  cost_management_config {
    enabled = true
  }

  gateway_api_config {
    channel = "CHANNEL_STANDARD"
  }

  binary_authorization {
    evaluation_mode = "DISABLED"
  }

  enable_shielded_nodes = true

  labels = local.cluster_labels

  lifecycle {
    ignore_changes = [
      logging_service,
      monitoring_service
    ]
  }
}

resource "google_container_node_pool" "primary_nodes" {
  provider = google-beta

  name       = "${var.cluster_name}-default"
  project    = var.project_id
  location   = var.region
  cluster    = google_container_cluster.this.name
  node_count = var.node_pool_initial_count

  node_config {
    machine_type    = var.node_pool_machine_type
    disk_size_gb    = var.node_pool_disk_size_gb
    image_type      = "COS_CONTAINERD"
    service_account = google_service_account.node_pool.email
    oauth_scopes = [
      "https://www.googleapis.com/auth/cloud-platform"
    ]
    labels = var.labels
    tags   = [var.cluster_name]
  }

  autoscaling {
    min_node_count = var.node_pool_min_count
    max_node_count = var.node_pool_max_count
  }

  management {
    auto_repair  = true
    auto_upgrade = true
  }

  timeouts {
    create = "30m"
    update = "30m"
    delete = "30m"
  }

  depends_on = [
    google_project_iam_member.node_roles,
    google_container_cluster.this
  ]
}
