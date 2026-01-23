# AWS RDS (PostgreSQL) Module

This Terraform module creates an AWS RDS PostgreSQL instance with associated resources.

## Features

- RDS PostgreSQL instance with configurable settings
- DB subnet group for multi-AZ deployment
- Security group with configurable access rules
- Optional custom parameter group
- Automatic backups with configurable retention
- CloudWatch logs integration
- Storage encryption support
- Multi-AZ deployment option
- Enhanced monitoring support

## Usage

```hcl
module "rds" {
  source = "../../modules/rds"

  identifier          = "crm-dev-db"
  engine_version      = "15.4"
  instance_class      = "db.t3.medium"
  allocated_storage   = 100

  database_name     = "crmdb"
  master_username   = "dbadmin"
  master_password   = var.db_password  # Use secrets manager

  vpc_id     = module.networking.vpc_id
  subnet_ids = module.networking.private_subnet_ids
  allowed_cidr_blocks = [module.networking.vpc_cidr]

  multi_az               = true
  deletion_protection    = true
  backup_retention_period = 7

  storage_encrypted = true

  tags = {
    Environment = "dev"
    Application = "crm"
  }
}
```

## Requirements

| Name | Version |
|------|---------|
| terraform | >= 1.5.0 |
| aws | >= 5.0 |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| identifier | Identifier for the RDS instance | `string` | n/a | yes |
| engine | Database engine type | `string` | `"postgres"` | no |
| engine_version | Database engine version | `string` | `"15.4"` | no |
| instance_class | Instance class for the RDS instance | `string` | `"db.t3.micro"` | no |
| allocated_storage | Allocated storage in GB | `number` | `20` | no |
| max_allocated_storage | Maximum storage autoscaling limit in GB | `number` | `100` | no |
| storage_type | Storage type (gp2, gp3, io1) | `string` | `"gp3"` | no |
| storage_encrypted | Enable storage encryption | `bool` | `true` | no |
| database_name | Name of the database to create | `string` | n/a | yes |
| master_username | Master username for the database | `string` | n/a | yes |
| master_password | Master password for the database | `string` | n/a | yes |
| vpc_id | VPC ID where the RDS instance will be created | `string` | n/a | yes |
| subnet_ids | List of subnet IDs for the DB subnet group | `list(string)` | n/a | yes |
| allowed_cidr_blocks | List of CIDR blocks allowed to access the RDS instance | `list(string)` | n/a | yes |
| multi_az | Enable Multi-AZ deployment | `bool` | `false` | no |
| backup_retention_period | Number of days to retain backups | `number` | `7` | no |
| deletion_protection | Enable deletion protection | `bool` | `true` | no |

See [variables.tf](./variables.tf) for complete list of variables.

## Outputs

| Name | Description |
|------|-------------|
| db_instance_id | ID of the RDS instance |
| db_instance_arn | ARN of the RDS instance |
| db_instance_endpoint | Connection endpoint for the RDS instance |
| db_instance_address | Address of the RDS instance |
| db_instance_port | Port of the RDS instance |
| db_instance_name | Name of the database |
| security_group_id | ID of the security group |

## Notes

- Default PostgreSQL version is 15.4
- Storage is encrypted by default
- Deletion protection is enabled by default for production safety
- Automatic backups are retained for 7 days by default
- CloudWatch logs are exported for postgresql and upgrade logs
- For production use, consider enabling Multi-AZ deployment
- Store master password in AWS Secrets Manager
