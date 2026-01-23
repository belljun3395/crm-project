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
  lifecycle_policy = var.lifecycle_policy_path != null ? file(var.lifecycle_policy_path) : null
}

resource "aws_ecr_repository" "this" {
  name                 = var.repository_name
  image_tag_mutability = var.image_tag_mutability

  image_scanning_configuration {
    scan_on_push = var.scan_on_push
  }

  encryption_configuration {
    encryption_type = "KMS"
  }

  tags = var.tags
}

resource "aws_ecr_lifecycle_policy" "this" {
  count = local.lifecycle_policy != null ? 1 : 0

  repository = aws_ecr_repository.this.name
  policy     = local.lifecycle_policy
}
