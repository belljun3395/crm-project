variable "project_id" {
  description = "GCP project ID."
  type        = string
}

variable "location" {
  description = "Artifact Registry location."
  type        = string
}

variable "repository_id" {
  description = "Artifact Registry repository ID."
  type        = string
}

variable "format" {
  description = "Artifact format, e.g. DOCKER."
  type        = string
  default     = "DOCKER"
}

variable "labels" {
  description = "Labels applied to the repository."
  type        = map(string)
  default     = {}
}
