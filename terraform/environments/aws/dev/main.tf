locals {
  common_tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "Terraform"
  }

  secrets_manager_secret_name = coalesce(
    var.secrets_manager_secret_name,
    "${var.project}/${var.environment}/application"
  )

  secrets_manager_description = coalesce(
    var.secrets_manager_description,
    "CRM ${var.environment} runtime configuration"
  )

  app_env_secret_values = {
    for key, value in var.app_env :
    key => tostring(value)
  }

  merged_secret_values = merge(local.app_env_secret_values, var.additional_secret_values)
}

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

module "ecr" {
  source = "../../../modules/ecr"

  repository_name = var.ecr_repository_name
  tags            = local.common_tags
}

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
  master_password         = var.rds_master_password
  vpc_id                  = module.networking.vpc_id
  subnet_ids              = module.networking.private_subnet_ids
  allowed_cidr_blocks     = var.rds_allowed_cidr_blocks
  multi_az                = var.rds_multi_az
  backup_retention_period = var.rds_backup_retention_period
  skip_final_snapshot     = var.rds_skip_final_snapshot
  deletion_protection     = var.rds_deletion_protection
  tags                    = local.common_tags
}

# Cache (Redis)
module "elasticache" {
  source = "../../../modules/elasticache"

  count = var.enable_elasticache ? 1 : 0

  cluster_id                 = "${var.cluster_name}-redis"
  description                = "Redis cluster for ${var.cluster_name}"
  engine_version             = var.elasticache_engine_version
  node_type                  = var.elasticache_node_type
  num_cache_clusters         = var.elasticache_num_cache_clusters
  vpc_id                     = module.networking.vpc_id
  subnet_ids                 = module.networking.private_subnet_ids
  allowed_cidr_blocks        = var.elasticache_allowed_cidr_blocks
  auth_token_enabled         = var.elasticache_auth_token_enabled
  auth_token                 = var.elasticache_auth_token
  transit_encryption_enabled = var.elasticache_transit_encryption_enabled
  at_rest_encryption_enabled = var.elasticache_at_rest_encryption_enabled
  snapshot_retention_limit   = var.elasticache_snapshot_retention_limit
  automatic_failover_enabled = var.elasticache_automatic_failover_enabled
  multi_az_enabled           = var.elasticache_multi_az_enabled
  tags                       = local.common_tags
}
