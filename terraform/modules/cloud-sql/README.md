# GCP Cloud SQL (PostgreSQL) Module

This Terraform module creates a GCP Cloud SQL PostgreSQL instance with associated database and user.

## Features

- Cloud SQL PostgreSQL instance with configurable version
- Automatic backups with point-in-time recovery
- Private IP support (VPC peering)
- High availability (REGIONAL) option
- SSL/TLS encryption support
- Query insights for performance monitoring
- Automatic storage scaling
- Maintenance window configuration
- Custom database flags support
- Deletion protection

## Usage

### Basic Configuration (Development)

```hcl
module "cloud_sql" {
  source = "../../modules/cloud-sql"

  instance_name    = "crm-dev-db"
  database_version = "POSTGRES_15"
  region           = var.region

  tier              = "db-f1-micro"
  availability_type = "ZONAL"
  disk_size         = 10

  database_name = "crmdb"
  db_user       = "dbadmin"
  db_password   = var.db_password

  # Private IP only
  ipv4_enabled    = false
  private_network = module.networking.network_self_link

  # Backups
  backup_enabled                 = true
  point_in_time_recovery_enabled = true
  backup_retention_count         = 7

  deletion_protection = false  # For dev environment
}
```

### Production Configuration

```hcl
module "cloud_sql_prod" {
  source = "../../modules/cloud-sql"

  instance_name    = "crm-prod-db"
  database_version = "POSTGRES_15"
  region           = var.region

  # High availability with better performance
  tier              = "db-custom-2-7680"  # 2 vCPU, 7.5 GB RAM
  availability_type = "REGIONAL"           # Multi-zone HA
  disk_type         = "PD_SSD"
  disk_size         = 100
  disk_autoresize   = true
  disk_autoresize_limit = 500

  database_name = "crmdb"
  db_user       = "dbadmin"
  db_password   = var.db_password

  # Network
  ipv4_enabled    = false
  private_network = module.networking.network_self_link
  require_ssl     = true

  # Backups
  backup_enabled                 = true
  point_in_time_recovery_enabled = true
  backup_retention_count         = 30
  transaction_log_retention_days = 7

  # Maintenance
  maintenance_window_day  = 7  # Sunday
  maintenance_window_hour = 4  # 4 AM

  # Performance monitoring
  query_insights_enabled = true

  # Database flags for optimization
  database_flags = [
    {
      name  = "max_connections"
      value = "200"
    },
    {
      name  = "shared_buffers"
      value = "524288"  # 4GB in 8KB pages
    },
    {
      name  = "work_mem"
      value = "16384"   # 128MB in KB
    }
  ]

  deletion_protection = true
}
```

## Requirements

| Name | Version |
|------|---------|
| terraform | >= 1.5.0 |
| google | >= 5.0 |
| random | >= 3.0 |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| instance_name | Base name for the Cloud SQL instance | `string` | n/a | yes |
| database_version | Database version (e.g., POSTGRES_15) | `string` | `"POSTGRES_15"` | no |
| region | GCP region for the instance | `string` | n/a | yes |
| tier | Machine type tier | `string` | `"db-f1-micro"` | no |
| availability_type | Availability type (ZONAL or REGIONAL) | `string` | `"ZONAL"` | no |
| disk_size | Disk size in GB | `number` | `10` | no |
| disk_autoresize | Enable automatic disk size increase | `bool` | `true` | no |
| database_name | Name of the database to create | `string` | n/a | yes |
| db_user | Database user name | `string` | n/a | yes |
| db_password | Database user password | `string` | n/a | yes |
| private_network | VPC network for private IP | `string` | `null` | no |
| backup_enabled | Enable automated backups | `bool` | `true` | no |
| point_in_time_recovery_enabled | Enable point-in-time recovery | `bool` | `true` | no |
| deletion_protection | Enable deletion protection | `bool` | `true` | no |

See [variables.tf](./variables.tf) for complete list of variables.

## Outputs

| Name | Description |
|------|-------------|
| instance_name | Name of the Cloud SQL instance |
| instance_connection_name | Connection name (project:region:instance) |
| private_ip_address | Private IP address of the instance |
| public_ip_address | Public IP address of the instance |
| database_name | Name of the created database |

## Notes

- Instance name must be globally unique (random suffix is added automatically)
- Default PostgreSQL version is 15
- SSL/TLS is required by default for security
- Deletion protection is enabled by default for production safety
- For private IP access, VPC peering must be configured
- Point-in-time recovery allows restoration to any point within the retention period
- REGIONAL availability provides automatic failover and high availability
- Query insights help identify slow queries and performance issues
- Store database password in GCP Secret Manager
- For production, use `db-custom-*` or `db-highmem-*` tiers for better performance

## Private IP Setup

To use private IP, you need to set up VPC peering:

1. Enable Service Networking API
2. Create a VPC peering connection:

```hcl
resource "google_compute_global_address" "private_ip_address" {
  name          = "private-ip-address"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = var.network_id
}

resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = var.network_id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_address.name]
}
```

Then pass the network ID to the module via `private_network` variable.
