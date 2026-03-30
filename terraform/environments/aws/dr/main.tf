locals {
  common_tags = {
    Project     = var.project
    Environment = "dr"
    ManagedBy   = "Terraform"
    DR_Role     = "secondary"
  }

  secrets_manager_secret_name = coalesce(
    var.secrets_manager_secret_name,
    "${var.project}/dr/application"
  )

  secrets_manager_description = coalesce(
    var.secrets_manager_description,
    "CRM DR runtime configuration"
  )

  generated_secret_values = merge(
    {
      CLOUD_PROVIDER                      = "aws"
      MAIL_PROVIDER                       = "ses"
      SCHEDULER_PROVIDER                  = "aws"
      MAIL_USERNAME                       = module.app_runtime.runtime_access_key_id
      MAIL_PASSWORD                       = module.app_runtime.runtime_secret_access_key
      AWS_ACCESS_KEY                      = module.app_runtime.runtime_access_key_id
      AWS_SECRET_KEY                      = module.app_runtime.runtime_secret_access_key
      AWS_CONFIGURATION_SET               = module.app_runtime.ses_configuration_set_name
      AWS_SCHEDULE_ROLE_ARN               = module.app_runtime.schedule_role_arn
      AWS_SCHEDULE_SQS_ARN                = module.app_runtime.schedule_queue_arn
      AWS_SCHEDULE_GROUP_NAME             = module.app_runtime.schedule_group_name
      AWS_SNS_CACHE_INVALIDATION_TOPIC_ARN = module.app_runtime.cache_invalidation_topic_arn
      KAFKA_BOOTSTRAP_SERVERS             = var.kafka_bootstrap_servers
    },
    var.enable_rds ? {
      DATABASE_URL = format(
        "r2dbc:pool:postgresql://%s:%s/%s",
        module.rds[0].db_instance_address,
        module.rds[0].db_instance_port,
        module.rds[0].db_instance_name
      )
      MASTER_DATABASE_URL = format(
        "r2dbc:pool:postgresql://%s:%s/%s",
        module.rds[0].db_instance_address,
        module.rds[0].db_instance_port,
        module.rds[0].db_instance_name
      )
      REPLICA_DATABASE_URL = format(
        "r2dbc:pool:postgresql://%s:%s/%s",
        module.rds[0].db_instance_address,
        module.rds[0].db_instance_port,
        module.rds[0].db_instance_name
      )
      DATABASE_USERNAME = var.rds_master_username
      DATABASE_PASSWORD = coalesce(var.rds_master_password, random_password.rds_master_password.result)
    } : {},
    var.enable_elasticache ? {
      REDIS_HOST          = module.elasticache[0].configuration_endpoint_address
      REDIS_MAX_REDIRECTS = "3"
      REDIS_PASSWORD      = coalesce(var.elasticache_auth_token, random_password.elasticache_auth_token.result)
      REDIS_NODES         = "${module.elasticache[0].configuration_endpoint_address}:${module.elasticache[0].port}"
    } : {}
  )

  merged_secret_values = merge(var.additional_secret_values, local.generated_secret_values)
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

resource "random_password" "rds_master_password" {
  length           = 24
  special          = true
  override_special = "!#$%&*()-_=+[]{}:?"
}

resource "random_password" "elasticache_auth_token" {
  length           = 32
  special          = true
  override_special = "!#$%&*()-_=+[]{}:?"
}

module "app_runtime" {
  source = "../../../modules/aws-app-runtime"

  name_prefix = var.cluster_name
  tags        = local.common_tags
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

# Database (PostgreSQL)
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
  database_name           = var.rds_database_name
  master_username         = var.rds_master_username
  master_password         = coalesce(var.rds_master_password, random_password.rds_master_password.result)
  vpc_id                  = module.networking.vpc_id
  subnet_ids              = module.networking.private_subnet_ids
  allowed_cidr_blocks        = var.rds_allowed_cidr_blocks
  allowed_security_group_ids = [module.eks.node_security_group_id]
  multi_az                = var.rds_multi_az
  backup_retention_period = var.rds_backup_retention_period
  skip_final_snapshot     = var.rds_skip_final_snapshot
  deletion_protection     = var.rds_deletion_protection

  tags = local.common_tags
}

# Cache (Redis)
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
  allowed_cidr_blocks        = var.elasticache_allowed_cidr_blocks
  allowed_security_group_ids = [module.eks.node_security_group_id]
  cluster_mode_enabled       = true
  auth_token_enabled         = var.elasticache_auth_token_enabled
  auth_token                 = coalesce(var.elasticache_auth_token, random_password.elasticache_auth_token.result)
  transit_encryption_enabled = var.elasticache_transit_encryption_enabled
  at_rest_encryption_enabled = var.elasticache_at_rest_encryption_enabled
  snapshot_retention_limit   = var.elasticache_snapshot_retention_limit
  automatic_failover_enabled = var.elasticache_automatic_failover_enabled
  multi_az_enabled           = var.elasticache_multi_az_enabled
  tags                       = local.common_tags
}

# Kafka (MSK)
module "msk" {
  source = "../../../modules/msk"
  count  = var.enable_kafka ? 1 : 0

  name_prefix            = var.cluster_name
  vpc_id                 = module.networking.vpc_id
  client_subnets         = module.networking.private_subnet_ids
  allowed_cidr_blocks    = concat([var.vpc_cidr], var.kafka_allowed_cidr_blocks)
  kafka_version          = var.kafka_version
  number_of_broker_nodes = var.kafka_number_of_brokers
  instance_type          = var.kafka_instance_type
  volume_size            = var.kafka_volume_size

  tags = local.common_tags
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
    path                = "/actuator/health"
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
