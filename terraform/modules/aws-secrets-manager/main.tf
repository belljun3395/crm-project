terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0"
    }
  }
}

locals {
  sorted_secret_keys   = sort(keys(var.secret_string_values))
  sorted_secret_values = { for k in local.sorted_secret_keys : k => var.secret_string_values[k] }
}

resource "aws_secretsmanager_secret" "this" {
  name                    = var.secret_name
  description             = var.description
  kms_key_id              = var.kms_key_id
  recovery_window_in_days = var.recovery_window_in_days

  tags = var.tags
}

resource "aws_secretsmanager_secret_version" "this" {
  secret_id     = aws_secretsmanager_secret.this.id
  secret_string = jsonencode(local.sorted_secret_values)
}
