terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.0"
    }
  }
}

# Subnet Group
resource "aws_elasticache_subnet_group" "this" {
  name       = "${var.cluster_id}-subnet-group"
  subnet_ids = var.subnet_ids

  tags = merge(
    var.tags,
    {
      Name = "${var.cluster_id}-subnet-group"
    }
  )
}

# Parameter Group
resource "aws_elasticache_parameter_group" "this" {
  count = var.create_parameter_group ? 1 : 0

  name   = "${var.cluster_id}-params"
  family = var.parameter_group_family

  dynamic "parameter" {
    for_each = var.parameters
    content {
      name  = parameter.value.name
      value = parameter.value.value
    }
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.cluster_id}-params"
    }
  )
}

# Security Group
resource "aws_security_group" "this" {
  name        = "${var.cluster_id}-elasticache-sg"
  description = "Security group for ${var.cluster_id} ElastiCache cluster"
  vpc_id      = var.vpc_id

  ingress {
    description = "Redis from VPC"
    from_port   = var.port
    to_port     = var.port
    protocol    = "tcp"
    cidr_blocks = var.allowed_cidr_blocks
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.cluster_id}-elasticache-sg"
    }
  )
}

# ElastiCache Replication Group (Cluster Mode)
resource "aws_elasticache_replication_group" "this" {
  replication_group_id = var.cluster_id
  description          = var.description

  # Engine
  engine             = "redis"
  engine_version     = var.engine_version
  node_type          = var.node_type
  num_cache_clusters = var.cluster_mode_enabled ? null : var.num_cache_clusters
  port               = var.port

  # Cluster Mode (AWS Provider v6+ uses direct arguments instead of block)
  num_node_groups         = var.cluster_mode_enabled ? var.num_node_groups : null
  replicas_per_node_group = var.cluster_mode_enabled ? var.replicas_per_node_group : null

  # Network
  subnet_group_name  = aws_elasticache_subnet_group.this.name
  security_group_ids = [aws_security_group.this.id]

  # Parameter Group
  parameter_group_name = var.create_parameter_group ? aws_elasticache_parameter_group.this[0].name : var.parameter_group_name

  # Auth
  auth_token                 = var.auth_token_enabled ? var.auth_token : null
  transit_encryption_enabled = var.transit_encryption_enabled
  at_rest_encryption_enabled = var.at_rest_encryption_enabled
  kms_key_id                 = var.at_rest_encryption_enabled ? var.kms_key_id : null

  # Backup
  snapshot_retention_limit = var.snapshot_retention_limit
  snapshot_window          = var.snapshot_window
  maintenance_window       = var.maintenance_window

  # Monitoring
  notification_topic_arn = var.notification_topic_arn

  # Additional
  automatic_failover_enabled = var.automatic_failover_enabled
  multi_az_enabled           = var.multi_az_enabled
  auto_minor_version_upgrade = var.auto_minor_version_upgrade
  apply_immediately          = var.apply_immediately

  # Logs
  dynamic "log_delivery_configuration" {
    for_each = var.log_delivery_configuration
    content {
      destination      = log_delivery_configuration.value.destination
      destination_type = log_delivery_configuration.value.destination_type
      log_format       = log_delivery_configuration.value.log_format
      log_type         = log_delivery_configuration.value.log_type
    }
  }

  tags = merge(
    var.tags,
    {
      Name = var.cluster_id
    }
  )
}
