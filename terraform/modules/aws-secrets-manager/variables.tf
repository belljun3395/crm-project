variable "secret_name" {
  description = "Name of the AWS Secrets Manager secret."
  type        = string
}

variable "description" {
  description = "Description for the secret."
  type        = string
  default     = null
}

variable "kms_key_id" {
  description = "Optional KMS key ID or ARN for encrypting the secret."
  type        = string
  default     = null
}

variable "recovery_window_in_days" {
  description = "Days before the secret is scheduled for deletion."
  type        = number
  default     = 30
}

variable "secret_string_values" {
  description = "Key/value pairs that will be stored as a JSON string in Secrets Manager."
  type        = map(string)
}

variable "tags" {
  description = "Tags applied to the secret."
  type        = map(string)
  default     = {}
}
