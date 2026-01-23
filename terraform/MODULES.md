# Terraform 모듈 레퍼런스

CRM 프로젝트의 재사용 가능한 Terraform 모듈 가이드

## 모듈 목록

### AWS 모듈 (7개)

| 모듈 | 설명 | 문서 |
|------|------|------|
| **networking** | VPC, 서브넷, NAT Gateway | [README](./modules/networking/README.md) |
| **eks** | EKS 클러스터 및 노드 그룹 | [README](./modules/eks/README.md) |
| **ecr** | ECR 컨테이너 레지스트리 | [README](./modules/ecr/README.md) |
| **aws-secrets-manager** | AWS Secrets Manager | [README](./modules/aws-secrets-manager/README.md) |
| **aws-ssm-parameters** | SSM Parameter Store | [README](./modules/aws-ssm-parameters/README.md) |
| **rds** | PostgreSQL 데이터베이스 | [README](./modules/rds/README.md) |
| **elasticache** | Redis 클러스터 | [README](./modules/elasticache/README.md) |

### GCP 모듈 (6개)

| 모듈 | 설명 | 문서 |
|------|------|------|
| **gcp-networking** | VPC, 서브넷, Cloud NAT | [README](./modules/gcp-networking/README.md) |
| **gke** | GKE 클러스터 및 노드 풀 | [README](./modules/gke/README.md) |
| **artifact_registry** | Artifact Registry | [README](./modules/artifact_registry/README.md) |
| **gcp-secret-manager** | Secret Manager | [README](./modules/gcp-secret-manager/README.md) |
| **cloud-sql** | PostgreSQL 데이터베이스 | [README](./modules/cloud-sql/README.md) |
| **memorystore** | Redis 인스턴스 | [README](./modules/memorystore/README.md) |

### DR 모듈 (2개)

| 모듈 | 설명 | 문서 |
|------|------|------|
| **aws-gcp-vpn** | AWS-GCP VPN 연결 | [README](./modules/aws-gcp-vpn/README.md) |
| **cloudflare-lb** | CloudFlare 글로벌 로드밸런서 | [README](./modules/cloudflare-lb/README.md) |

---

## 주요 모듈 사용법

### RDS (AWS PostgreSQL)

```hcl
module "rds" {
  source = "../../modules/rds"

  identifier        = "crm-postgres"
  engine_version    = "15.4"
  instance_class    = "db.t3.medium"
  allocated_storage = 100
  
  database_name = "crm"
  username      = "crmadmin"
  password      = var.db_password
  
  vpc_id     = module.networking.vpc_id
  subnet_ids = module.networking.private_subnet_ids
  
  multi_az                = true
  backup_retention_period = 30
  
  # Logical Replication 활성화 (DR용)
  parameters = [
    {
      name  = "rds.logical_replication"
      value = "1"
    }
  ]
}
```

### Cloud SQL (GCP PostgreSQL)

```hcl
module "cloud_sql" {
  source = "../../modules/cloud-sql"

  project_id       = var.project_id
  name             = "crm-postgres"
  region           = "asia-northeast3"
  database_version = "POSTGRES_15"
  tier             = "db-custom-4-15360"
  
  database_name = "crm"
  user_name     = "crmadmin"
  user_password = var.db_password
  
  availability_type              = "REGIONAL"
  backup_enabled                 = true
  point_in_time_recovery_enabled = true
}
```

### ElastiCache (AWS Redis)

```hcl
module "elasticache" {
  source = "../../modules/elasticache"

  cluster_id         = "crm-redis"
  engine_version     = "7.0"
  node_type          = "cache.t3.medium"
  num_cache_clusters = 3
  
  vpc_id     = module.networking.vpc_id
  subnet_ids = module.networking.private_subnet_ids
  
  auth_token_enabled             = true
  auth_token                     = var.redis_password
  transit_encryption_enabled     = true
  at_rest_encryption_enabled     = true
  automatic_failover_enabled     = true
  multi_az_enabled               = true
}
```

### Memorystore (GCP Redis)

```hcl
module "memorystore" {
  source = "../../modules/memorystore"

  instance_id        = "crm-redis"
  region             = "asia-northeast3"
  tier               = "STANDARD_HA"
  memory_size_gb     = 5
  redis_version      = "REDIS_7_0"
  replica_count      = 2
  
  authorized_network      = module.networking.network_id
  auth_enabled            = true
  transit_encryption_mode = "SERVER_AUTHENTICATION"
  persistence_mode        = "RDB"
}
```

### AWS-GCP VPN

```hcl
module "vpn" {
  source = "../../modules/aws-gcp-vpn"

  name_prefix                 = "crm-dr"
  aws_vpc_id                  = module.aws_networking.vpc_id
  aws_private_route_table_ids = module.aws_networking.private_route_table_ids
  gcp_network_id              = module.gcp_networking.network_id
  gcp_region                  = "asia-northeast3"
  
  tunnel1_preshared_key = var.vpn_key1
  tunnel2_preshared_key = var.vpn_key2
  
  advertised_ip_ranges = ["10.20.0.0/20"]
}
```

### CloudFlare Load Balancer

```hcl
module "cloudflare_lb" {
  source = "../../modules/cloudflare-lb"

  name_prefix           = "crm-dr"
  cloudflare_account_id = var.cf_account_id
  cloudflare_zone_id    = var.cf_zone_id
  domain_name           = "crm.example.com"
  
  # AWS Origin
  aws_origins = [{
    name    = "aws-primary"
    address = module.aws_alb.dns_name
    enabled = true
    weight  = 1.0
  }]
  
  # GCP Origin
  gcp_origins = [{
    name    = "gcp-secondary"
    address = module.gcp_lb.ip_address
    enabled = true
    weight  = 1.0
  }]
  
  steering_policy = "dynamic_latency"
  enable_failover = true
}
```

---

## 모듈 변수 패턴

### 공통 변수

모든 모듈은 다음 패턴을 따릅니다:

```hcl
# 필수 변수
variable "name" {
  description = "리소스 이름"
  type        = string
}

# 선택적 변수 (기본값 제공)
variable "enabled" {
  description = "리소스 활성화 여부"
  type        = bool
  default     = true
}

# 민감한 변수
variable "password" {
  description = "비밀번호"
  type        = string
  sensitive   = true
}

# 태그/라벨
variable "tags" {  # AWS
  description = "리소스 태그"
  type        = map(string)
  default     = {}
}

variable "labels" {  # GCP
  description = "리소스 라벨"
  type        = map(string)
  default     = {}
}
```

### 출력값 패턴

```hcl
output "id" {
  description = "리소스 ID"
  value       = resource.id
}

output "endpoint" {
  description = "연결 엔드포인트"
  value       = resource.endpoint
}

output "connection_string" {
  description = "연결 문자열"
  value       = "..."
  sensitive   = true  # 민감한 정보는 sensitive 설정
}
```

---

## 보안 권장사항

### 시크릿 관리

**하지 말 것:**
```hcl
# 코드에 직접 하드코딩
password = "my_password"  # 절대 금지!
```

**올바른 방법:**
```hcl
# 변수로 전달
password = var.db_password

# terraform.tfvars는 .gitignore에 포함
# 또는 환경 변수 사용
# export TF_VAR_db_password="..."
```

### 상태 파일 보안

```hcl
# backend.tf
terraform {
  backend "remote" {
    organization = "your-org"
    workspaces {
      name = "crm-prod"
    }
  }
}

# 상태 파일에는 민감한 정보가 포함되므로
# 암호화된 원격 스토리지 사용 필수
```

---

## 추가 리소스

- [Terraform 모듈 작성 가이드](https://www.terraform.io/docs/modules/)
- [AWS Terraform Provider](https://registry.terraform.io/providers/hashicorp/aws/)
- [GCP Terraform Provider](https://registry.terraform.io/providers/hashicorp/google/)
- [CloudFlare Terraform Provider](https://registry.terraform.io/providers/cloudflare/cloudflare/)

---

**최종 업데이트**: 2025년 1월  
**버전**: 2.0
