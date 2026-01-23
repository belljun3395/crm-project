variable "secret_name" {
  description = "Name of the GCP Secret Manager secret."
  type        = string
}

variable "secret_string_values" {
  description = "Key/value pairs that will be stored as a JSON string in Secret Manager."
  type        = map(string)
}

variable "labels" {
  description = "Labels applied to the secret."
  type        = map(string)
  default     = {}
}
