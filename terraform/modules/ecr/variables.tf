variable "repository_name" {
  description = "Name of the ECR repository."
  type        = string
}

variable "image_tag_mutability" {
  description = "Tag mutability setting for the repository. Either MUTABLE or IMMUTABLE."
  type        = string
  default     = "IMMUTABLE"
}

variable "scan_on_push" {
  description = "Enable image scanning on push."
  type        = bool
  default     = true
}

variable "lifecycle_policy_path" {
  description = "Optional path to a lifecycle policy JSON file."
  type        = string
  default     = null
}

variable "tags" {
  description = "Common tags applied to the repository."
  type        = map(string)
  default     = {}
}
