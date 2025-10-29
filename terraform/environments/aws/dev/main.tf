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
  source = "../../modules/networking"

  name                 = var.cluster_name
  vpc_cidr             = var.vpc_cidr
  azs                  = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  enable_nat           = var.enable_nat_gateway
  tags                 = local.common_tags
}

module "eks" {
  source = "../../modules/eks"

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
  source = "../../modules/ecr"

  repository_name = var.ecr_repository_name
  tags            = local.common_tags
}

module "app_secret" {
  source = "../../modules/aws-secrets-manager"

  secret_name             = local.secrets_manager_secret_name
  description             = local.secrets_manager_description
  kms_key_id              = var.secrets_manager_kms_key_id
  recovery_window_in_days = var.secrets_manager_recovery_window_in_days
  secret_string_values    = local.merged_secret_values
  tags                    = local.common_tags
}
