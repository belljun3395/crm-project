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
  docker_endpoint          = "${var.location}-docker.pkg.dev/${var.project_id}/${var.repository_id}"
  effective_repository_url = var.format == "DOCKER" ? local.docker_endpoint : google_artifact_registry_repository.this.name
}

resource "google_artifact_registry_repository" "this" {
  provider = google

  project       = var.project_id
  location      = var.location
  repository_id = var.repository_id
  description   = "Container images for CRM project."
  format        = var.format
  labels        = var.labels
}
