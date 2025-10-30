locals {
  common_tags = {
    Project     = var.project
    Environment = "dr"
    ManagedBy   = "Terraform"
    DR_Role     = "active" # Active-Active 구성
  }

  secrets_manager_secret_name = coalesce(
    var.secrets_manager_secret_name,
    "${var.project}/dr/application"
  )

  secrets_manager_description = coalesce(
    var.secrets_manager_description,
    "CRM DR runtime configuration"
  )

  app_env_secret_values = {
    for key, value in var.app_env :
    key => tostring(value)
  }

  merged_secret_values = merge(local.app_env_secret_values, var.additional_secret_values)
}

# Networking
module "networking" {
  source = "../../../modules/networking"

  name                 = var.cluster_name
  vpc_cidr             = var.vpc_cidr
  azs                  = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  enable_nat           = var.enable_nat_gateway
  tags                 = local.common_tags
}

# EKS Cluster
module "eks" {
  source = "../../../modules/eks"

  cluster_name                          = var.cluster_name
  cluster_version                       = var.cluster_version
  vpc_id                                = module.networking.vpc_id
  private_subnet_ids                    = module.networking.private_subnet_ids
  public_subnet_ids                     = module.networking.public_subnet_ids
  enable_cluster_public_access          = var.enable_cluster_public_access
  enable_cluster_private_access         = true
  cluster_public_access_cidrs           = var.cluster_public_access_cidrs
  cluster_enabled_log_types             = var.cluster_enabled_log_types
  node_group_instance_types             = var.node_group_instance_types
  node_group_desired_capacity           = var.node_group_desired_capacity
  node_group_min_size                   = var.node_group_min_size
  node_group_max_size                   = var.node_group_max_size
  node_group_capacity_type              = var.node_group_capacity_type
  cluster_additional_security_group_ids = []
  tags                                  = local.common_tags
}

# ECR
module "ecr" {
  source = "../../../modules/ecr"

  repository_name = var.ecr_repository_name
  tags            = local.common_tags
}

# Secrets Manager
module "app_secret" {
  source = "../../../modules/aws-secrets-manager"

  secret_name             = local.secrets_manager_secret_name
  description             = local.secrets_manager_description
  kms_key_id              = var.secrets_manager_kms_key_id
  recovery_window_in_days = var.secrets_manager_recovery_window_in_days
  secret_string_values    = local.merged_secret_values
  tags                    = local.common_tags
}

# Database (PostgreSQL) - Primary for Active-Active
module "rds" {
  source = "../../../modules/rds"

  count = var.enable_rds ? 1 : 0

  identifier              = "${var.cluster_name}-postgres"
  engine                  = "postgres"
  engine_version          = var.rds_engine_version
  instance_class          = var.rds_instance_class
  allocated_storage       = var.rds_allocated_storage
  storage_encrypted       = var.rds_storage_encrypted
  kms_key_id              = var.rds_kms_key_id
  db_name                 = var.rds_database_name
  username                = var.rds_master_username
  password                = var.rds_master_password
  vpc_id                  = module.networking.vpc_id
  subnet_ids              = module.networking.private_subnet_ids
  allowed_cidr_blocks     = concat(var.rds_allowed_cidr_blocks, [var.gcp_vpc_cidr])
  multi_az                = var.rds_multi_az
  backup_retention_period = var.rds_backup_retention_period
  skip_final_snapshot     = var.rds_skip_final_snapshot
  deletion_protection     = var.rds_deletion_protection

  # Logical Replication 활성화 (GCP 복제를 위해)
  parameters = [
    {
      name  = "rds.logical_replication"
      value = "1"
    },
    {
      name  = "wal_sender_timeout"
      value = "0"
    },
    {
      name  = "max_wal_senders"
      value = "10"
    },
    {
      name  = "max_replication_slots"
      value = "10"
    }
  ]

  tags = local.common_tags
}

# Cache (Redis) - Primary for Active-Active
module "elasticache" {
  source = "../../../modules/elasticache"

  count = var.enable_elasticache ? 1 : 0

  cluster_id                 = "${var.cluster_name}-redis"
  description                = "Redis cluster for ${var.cluster_name} (DR)"
  engine_version             = var.elasticache_engine_version
  node_type                  = var.elasticache_node_type
  num_cache_clusters         = var.elasticache_num_cache_clusters
  vpc_id                     = module.networking.vpc_id
  subnet_ids                 = module.networking.private_subnet_ids
  allowed_cidr_blocks        = concat(var.elasticache_allowed_cidr_blocks, [var.gcp_vpc_cidr])
  auth_token_enabled         = var.elasticache_auth_token_enabled
  auth_token                 = var.elasticache_auth_token
  transit_encryption_enabled = var.elasticache_transit_encryption_enabled
  at_rest_encryption_enabled = var.elasticache_at_rest_encryption_enabled
  snapshot_retention_limit   = var.elasticache_snapshot_retention_limit
  automatic_failover_enabled = var.elasticache_automatic_failover_enabled
  multi_az_enabled           = var.elasticache_multi_az_enabled
  tags                       = local.common_tags
}

# VPN Connection to GCP (조건부)
module "vpn_to_gcp" {
  source = "../../../modules/aws-gcp-vpn"

  count = var.enable_vpn ? 1 : 0

  name_prefix                 = var.cluster_name
  aws_vpc_id                  = module.networking.vpc_id
  aws_private_route_table_ids = module.networking.private_route_table_ids
  gcp_network_id              = var.gcp_network_id
  gcp_region                  = var.gcp_region
  tunnel1_preshared_key       = var.vpn_tunnel1_preshared_key
  tunnel2_preshared_key       = var.vpn_tunnel2_preshared_key
  advertised_ip_ranges        = [var.gcp_vpc_cidr]
  tags                        = local.common_tags

  depends_on = [module.networking]
}

# Application Load Balancer (CloudFlare와 연결)
resource "aws_lb" "main" {
  name               = "${var.cluster_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = module.networking.public_subnet_ids

  enable_deletion_protection       = var.alb_deletion_protection
  enable_http2                     = true
  enable_cross_zone_load_balancing = true

  tags = merge(
    local.common_tags,
    {
      Name = "${var.cluster_name}-alb"
    }
  )
}

resource "aws_security_group" "alb" {
  name        = "${var.cluster_name}-alb-sg"
  description = "Security group for ALB"
  vpc_id      = module.networking.vpc_id

  ingress {
    description = "HTTPS from Internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP from Internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${var.cluster_name}-alb-sg"
    }
  )
}

resource "aws_lb_target_group" "app" {
  name     = "${var.cluster_name}-tg"
  port     = 80
  protocol = "HTTP"
  vpc_id   = module.networking.vpc_id

  health_check {
    enabled             = true
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 30
    path                = "/health"
    matcher             = "200"
  }

  tags = merge(
    local.common_tags,
    {
      Name = "${var.cluster_name}-tg"
    }
  )
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.main.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = var.acm_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}
