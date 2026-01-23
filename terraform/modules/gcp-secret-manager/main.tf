terraform {
  required_version = ">= 1.5.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 5.0"
    }
  }
}

locals {
  sorted_secret_keys   = sort(keys(var.secret_string_values))
  sorted_secret_values = { for k in local.sorted_secret_keys : k => var.secret_string_values[k] }
}

resource "google_secret_manager_secret" "this" {
  secret_id = var.secret_name

  labels = var.labels

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "this" {
  secret      = google_secret_manager_secret.this.id
  secret_data = jsonencode(local.sorted_secret_values)
}
