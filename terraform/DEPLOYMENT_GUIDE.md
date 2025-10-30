# 🚀 Active-Active 멀티 클라우드 DR 완전 구현 가이드

## 📋 목차

1. [아키텍처 개요](#아키텍처-개요)
2. [생성된 모듈 및 환경](#생성된-모듈-및-환경)
3. [단계별 구현](#단계별-구현)
4. [설정 파일 예시](#설정-파일-예시)
5. [배포 순서](#배포-순서)
6. [테스트 시나리오](#테스트-시나리오)
7. [모니터링 및 알람](#모니터링-및-알람)

---

## 🏗️ 아키텍처 개요

### Active-Active 구성

```
                    CloudFlare 글로벌 로드밸런서
                    (트래픽 분산 50:50)
                             │
              ┌──────────────┴──────────────┐
              ▼                             ▼
        AWS (Active)                  GCP (Active)
        ┌─────────────┐              ┌─────────────┐
        │ EKS Cluster │              │ GKE Cluster │
        │ 50% 트래픽   │              │ 50% 트래픽   │
        ├─────────────┤              ├─────────────┤
        │ RDS         │◀─────────────│ Cloud SQL   │
        │ (Primary)   │  Logical     │ (Replica)   │
        │ Read/Write  │  Replication │ Read-Only   │
        ├─────────────┤              ├─────────────┤
        │ElastiCache  │◀─ App Level ─│Memorystore  │
        │Read/Write   │  Sync        │Read/Write   │
        └─────────────┘              └─────────────┘
               ▲                             ▲
               └──────── VPN Tunnel ─────────┘
                    (프라이빗 통신)
```

### 장애 시나리오

**AWS 장애 발생:**
1. CloudFlare health check 실패 감지 (30초)
2. 자동으로 100% 트래픽을 GCP로 전환
3. GCP Cloud SQL을 Primary로 수동 승격
4. 서비스 계속 (복구 시간: 2-5분)

**GCP 장애 발생:**
1. CloudFlare health check 실패 감지 (30초)
2. 자동으로 100% 트래픽을 AWS로 전환
3. AWS가 이미 Primary이므로 추가 작업 불필요
4. 서비스 계속 (복구 시간: 30초-2분)

---

## 📦 생성된 모듈 및 환경

### 새로 생성된 모듈

```
terraform/modules/
├── aws-gcp-vpn/            ✨ NEW - AWS-GCP VPN 연결
│   ├── main.tf
│   ├── variables.tf
│   └── outputs.tf
│
└── cloudflare-lb/          ✨ NEW - CloudFlare 글로벌 로드밸런서
    ├── main.tf
    ├── variables.tf
    └── outputs.tf
```

### 새로 생성된 환경

```
terraform/environments/
├── aws/dr/                 ✨ NEW - AWS DR 환경
│   ├── backend.tf
│   ├── providers.tf
│   ├── main.tf            # VPN, ALB 포함
│   ├── variables.tf       # VPN 변수 포함
│   └── outputs.tf
│
└── gcp/dr/                 ✨ NEW - GCP DR 환경
    ├── backend.tf
    ├── providers.tf
    ├── main.tf            # VPN 포함
    ├── variables.tf       # VPN 변수 포함
    └── outputs.tf
```

---

## 🚀 단계별 구현

### Phase 1: 사전 준비 (1-2일)

#### 1.1 CloudFlare 계정 설정

```bash
# CloudFlare API 토큰 생성
# 1. CloudFlare 대시보드 로그인
# 2. My Profile > API Tokens
# 3. Create Token
# 4. 권한: Zone.DNS, Zone.Load Balancers (Edit)

export CLOUDFLARE_API_TOKEN="your_token_here"
export CLOUDFLARE_ACCOUNT_ID="your_account_id"
export CLOUDFLARE_ZONE_ID="your_zone_id"
```

#### 1.2 VPN 사전 공유 키 생성

```bash
# 강력한 사전 공유 키 생성
openssl rand -base64 32 > vpn_tunnel1_key.txt
openssl rand -base64 32 > vpn_tunnel2_key.txt

# 주의: 이 키들을 안전하게 보관하세요!
```

#### 1.3 ACM 인증서 생성 (AWS)

```bash
# AWS Certificate Manager에서 도메인 인증서 요청
aws acm request-certificate \
  --domain-name crm.example.com \
  --validation-method DNS \
  --region ap-northeast-2

# 인증서 ARN 저장
export ACM_CERTIFICATE_ARN="arn:aws:acm:..."
```

---

### Phase 2: AWS DR 환경 배포 (1주)

#### 2.1 Terraform 변수 설정

```bash
cd terraform/environments/aws/dr

cat > terraform.tfvars << 'EOF'
# 기본 설정
aws_region   = "ap-northeast-2"
cluster_name = "crm-dr"
project      = "crm"
environment  = "dr"

# VPC 설정
vpc_cidr             = "10.30.0.0/16"
availability_zones   = ["ap-northeast-2a", "ap-northeast-2c"]
public_subnet_cidrs  = ["10.30.0.0/24", "10.30.1.0/24"]
private_subnet_cidrs = ["10.30.10.0/24", "10.30.11.0/24"]

# EKS 설정
cluster_version             = "1.29"
node_group_instance_types   = ["t3.xlarge"]
node_group_desired_capacity = 5
node_group_min_size         = 3
node_group_max_size         = 10

# RDS 설정 (활성화)
enable_rds                  = true
rds_engine_version          = "15.4"
rds_instance_class          = "db.t3.large"
rds_allocated_storage       = 200
rds_storage_encrypted       = true
rds_database_name           = "crm"
rds_master_username         = "crmadmin"
rds_master_password         = "CHANGE_ME_STRONG_PASSWORD"
rds_multi_az                = true
rds_backup_retention_period = 30
rds_deletion_protection     = true
rds_skip_final_snapshot     = false

# ElastiCache 설정 (활성화)
enable_elasticache                     = true
elasticache_engine_version             = "7.0"
elasticache_node_type                  = "cache.t3.medium"
elasticache_num_cache_clusters         = 3
elasticache_auth_token_enabled         = true
elasticache_auth_token                 = "CHANGE_ME_STRONG_TOKEN_MIN_16_CHARS"
elasticache_transit_encryption_enabled = true
elasticache_at_rest_encryption_enabled = true
elasticache_snapshot_retention_limit   = 30
elasticache_automatic_failover_enabled = true
elasticache_multi_az_enabled           = true

# VPN 설정 (나중에 GCP 배포 후 활성화)
enable_vpn                  = false  # 먼저 false로 시작
gcp_network_id              = ""     # GCP 배포 후 채움
gcp_region                  = "asia-northeast3"
gcp_vpc_cidr                = "10.20.0.0/20"
vpn_tunnel1_preshared_key   = ""     # 나중에 설정
vpn_tunnel2_preshared_key   = ""     # 나중에 설정

# ALB 설정
alb_deletion_protection = true
acm_certificate_arn     = "arn:aws:acm:ap-northeast-2:..."  # ACM ARN

# 애플리케이션 시크릿
app_env = {
  DATABASE_URL            = "postgresql://crmadmin:password@localhost:5432/crm"
  DATABASE_USERNAME       = "crmadmin"
  DATABASE_PASSWORD       = "CHANGE_ME"
  REDIS_HOST              = "localhost"
  REDIS_PASSWORD          = "CHANGE_ME"
  REDIS_MAX_REDIRECTS     = "5"
  REDIS_NODES             = ""
  MAIL_USERNAME           = "your_email"
  MAIL_PASSWORD           = "your_password"
  AWS_ACCESS_KEY          = "your_key"
  AWS_SECRET_KEY          = "your_secret"
  AWS_CONFIGURATION_SET   = "your_config"
  AWS_SCHEDULE_ROLE_ARN   = "your_arn"
  AWS_SCHEDULE_SQS_ARN    = "your_arn"
  AWS_SCHEDULE_GROUP_NAME = "your_group"
  KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
  SCHEDULER_PROVIDER      = "aws"
}
EOF
```

#### 2.2 AWS 배포

```bash
# Terraform 초기화
terraform init

# 계획 확인
terraform plan

# 배포 (약 20-30분 소요)
terraform apply

# ALB DNS 이름 저장
terraform output alb_dns_name
# 출력 예: crm-dr-alb-1234567890.ap-northeast-2.elb.amazonaws.com
```

---

### Phase 3: GCP DR 환경 배포 (1주)

#### 3.1 Terraform 변수 설정

```bash
cd terraform/environments/gcp/dr

cat > terraform.tfvars << 'EOF'
# 기본 설정
project_id  = "your-gcp-project-id"
region      = "asia-northeast3"
cluster_name = "crm-dr-gke"

# VPC 설정
network_name                   = "crm-dr"
subnet_ip_cidr_range           = "10.20.0.0/20"
subnet_secondary_pods_cidr     = "10.21.0.0/16"
subnet_secondary_services_cidr = "10.22.0.0/20"
ip_range_pods_name             = "crm-dr-pods"
ip_range_services_name         = "crm-dr-services"

# GKE 설정
cluster_release_channel     = "REGULAR"
cluster_enable_private_nodes = true
node_pool_machine_type      = "n2-standard-16"
node_pool_disk_size_gb      = 200
node_pool_min_count         = 3
node_pool_max_count         = 10
node_pool_initial_count     = 5

# Secret Manager (활성화)
enable_secret_manager          = true
secret_manager_secret_id       = "crm-dr-application"
secret_manager_secret_data     = jsonencode({
  DATABASE_URL  = "postgresql://..."
  REDIS_HOST    = "..."
  # ... 동일한 시크릿
})

# Cloud SQL (활성화 - Replica 모드)
enable_cloud_sql                           = true
cloud_sql_instance_name                    = "crm-dr-postgres"
cloud_sql_database_version                 = "POSTGRES_15"
cloud_sql_tier                             = "db-custom-4-15360"
cloud_sql_disk_size                        = 200
cloud_sql_disk_type                        = "PD_SSD"
cloud_sql_availability_type                = "REGIONAL"
cloud_sql_database_name                    = "crm"
cloud_sql_user_name                        = "crmadmin"
cloud_sql_user_password                    = "CHANGE_ME_STRONG_PASSWORD"
cloud_sql_backup_enabled                   = true
cloud_sql_point_in_time_recovery_enabled   = true
cloud_sql_deletion_protection              = true

# Memorystore (활성화)
enable_memorystore                = true
memorystore_instance_id           = "crm-dr-redis"
memorystore_display_name          = "CRM DR Redis"
memorystore_tier                  = "STANDARD_HA"
memorystore_memory_size_gb        = 10
memorystore_redis_version         = "REDIS_7_0"
memorystore_replica_count         = 2
memorystore_auth_enabled          = true
memorystore_transit_encryption_mode = "SERVER_AUTHENTICATION"
memorystore_persistence_mode      = "RDB"
memorystore_rdb_snapshot_period   = "SIX_HOURS"

# Labels
labels = {
  project     = "crm"
  environment = "dr"
  managed_by  = "terraform"
  role        = "active"
}
EOF
```

#### 3.2 GCP 배포

```bash
# Terraform 초기화
terraform init

# 계획 확인
terraform plan

# 배포 (약 20-30분 소요)
terraform apply

# 출력값 확인 및 저장
terraform output cloud_sql_connection_name
terraform output memorystore_host
```

---

### Phase 4: VPN 연결 구성 (3-5일)

#### 4.1 GCP 네트워크 정보 가져오기

```bash
cd terraform/environments/gcp/dr

# GCP 네트워크 ID 가져오기
export GCP_NETWORK_ID=$(terraform output -raw network_name)

# 형식: projects/your-project/global/networks/crm-dr
echo "projects/your-gcp-project-id/global/networks/${GCP_NETWORK_ID}"
```

#### 4.2 AWS VPN 활성화

```bash
cd terraform/environments/aws/dr

# terraform.tfvars 수정
cat >> terraform.tfvars << EOF

# VPN 활성화
enable_vpn                = true
gcp_network_id            = "projects/your-gcp-project-id/global/networks/crm-dr"
vpn_tunnel1_preshared_key = "$(cat ~/vpn_tunnel1_key.txt)"
vpn_tunnel2_preshared_key = "$(cat ~/vpn_tunnel2_key.txt)"
EOF

# VPN 배포
terraform apply
```

#### 4.3 VPN 상태 확인

```bash
# AWS측
aws ec2 describe-vpn-connections \
  --filters "Name=tag:Name,Values=crm-dr-vpn-tunnel-*" \
  --query 'VpnConnections[*].[VpnConnectionId,State]'

# GCP측
gcloud compute vpn-tunnels list --filter="name~crm-dr"
```

---

### Phase 5: 데이터베이스 복제 설정 (1주)

#### 5.1 PostgreSQL Logical Replication 설정

```sql
-- AWS RDS에 접속
psql -h <rds-endpoint> -U crmadmin -d crm

-- Publication 생성 (모든 테이블 복제)
CREATE PUBLICATION crm_replication FOR ALL TABLES;

-- Replication 슬롯 생성
SELECT pg_create_logical_replication_slot('crm_slot', 'pgoutput');

-- 복제 사용자 권한 확인
GRANT SELECT ON ALL TABLES IN SCHEMA public TO crmadmin;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO crmadmin;
```

```sql
-- GCP Cloud SQL에 접속
psql -h <cloudsql-ip> -U crmadmin -d crm

-- Subscription 생성 (VPN을 통한 프라이빗 연결)
CREATE SUBSCRIPTION crm_subscription
CONNECTION 'host=<aws-rds-private-ip> port=5432 dbname=crm user=crmadmin password=xxx sslmode=require'
PUBLICATION crm_replication
WITH (slot_name = 'crm_slot', create_slot = false);

-- 복제 상태 확인
SELECT * FROM pg_stat_subscription;
```

#### 5.2 복제 모니터링

```sql
-- AWS RDS에서 복제 지연 확인
SELECT slot_name, active, restart_lsn, confirmed_flush_lsn 
FROM pg_replication_slots;

-- GCP Cloud SQL에서 복제 상태 확인
SELECT subname, status, received_lsn, latest_end_lsn 
FROM pg_stat_subscription;
```

---

### Phase 6: CloudFlare 글로벌 로드밸런서 설정 (3-5일)

#### 6.1 CloudFlare 프로바이더 설정

```bash
# 별도 디렉터리 생성 (글로벌 리소스)
mkdir -p terraform/environments/global/cloudflare-lb

cat > terraform/environments/global/cloudflare-lb/main.tf << 'EOF'
terraform {
  required_version = ">= 1.5.0"
  required_providers {
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 4.0"
    }
  }
}

provider "cloudflare" {
  api_token = var.cloudflare_api_token
}

module "global_lb" {
  source = "../../../modules/cloudflare-lb"

  name_prefix           = "crm-dr"
  cloudflare_account_id = var.cloudflare_account_id
  cloudflare_zone_id    = var.cloudflare_zone_id
  domain_name           = var.domain_name
  dns_record_name       = "@"  # 루트 도메인 또는 "app" 등

  # Health Check
  health_check_type          = "https"
  health_check_port          = 443
  health_check_path          = "/health"
  health_check_interval      = 60
  health_check_timeout       = 5
  health_check_retries       = 2
  health_check_expected_codes = "200"

  # AWS Origin
  aws_origins = [
    {
      name    = "aws-primary"
      address = var.aws_alb_dns_name
      enabled = true
      weight  = 1.0
    }
  ]

  # GCP Origin
  gcp_origins = [
    {
      name    = "gcp-secondary"
      address = var.gcp_lb_ip
      enabled = true
      weight  = 1.0
    }
  ]

  # Load Balancing Strategy
  steering_policy    = "dynamic_latency"  # 지연시간 기반 라우팅
  session_affinity   = "cookie"
  session_affinity_ttl = 3600

  # Failover
  enable_failover = true
  primary_cloud   = "aws"

  # CloudFlare Proxy
  proxied = true

  # Notifications
  notification_email = var.notification_email
}
EOF

cat > terraform/environments/global/cloudflare-lb/variables.tf << 'EOF'
variable "cloudflare_api_token" {
  description = "CloudFlare API token"
  type        = string
  sensitive   = true
}

variable "cloudflare_account_id" {
  description = "CloudFlare account ID"
  type        = string
}

variable "cloudflare_zone_id" {
  description = "CloudFlare zone ID"
  type        = string
}

variable "domain_name" {
  description = "Domain name"
  type        = string
}

variable "aws_alb_dns_name" {
  description = "AWS ALB DNS name"
  type        = string
}

variable "gcp_lb_ip" {
  description = "GCP Load Balancer IP"
  type        = string
}

variable "notification_email" {
  description = "Email for notifications"
  type        = string
}
EOF

cat > terraform/environments/global/cloudflare-lb/terraform.tfvars << 'EOF'
cloudflare_api_token   = "your_cloudflare_api_token"
cloudflare_account_id  = "your_account_id"
cloudflare_zone_id     = "your_zone_id"
domain_name            = "crm.example.com"
aws_alb_dns_name       = "crm-dr-alb-xxx.ap-northeast-2.elb.amazonaws.com"
gcp_lb_ip              = "34.123.45.67"
notification_email     = "ops@example.com"
EOF
```

#### 6.2 CloudFlare LB 배포

```bash
cd terraform/environments/global/cloudflare-lb

terraform init
terraform plan
terraform apply
```

---

## ✅ 완료 체크리스트

```
Phase 1: 사전 준비
  ✅ CloudFlare 계정 및 API 토큰
  ✅ VPN 사전 공유 키 생성
  ✅ ACM 인증서 발급

Phase 2: AWS DR 환경
  ✅ VPC 및 네트워크 구성
  ✅ EKS 클러스터 배포
  ✅ RDS PostgreSQL (Primary)
  ✅ ElastiCache Redis
  ✅ ALB 구성

Phase 3: GCP DR 환경
  ✅ VPC 및 네트워크 구성
  ✅ GKE 클러스터 배포
  ✅ Cloud SQL PostgreSQL (Replica)
  ✅ Memorystore Redis
  ✅ Load Balancer

Phase 4: VPN 연결
  ✅ AWS VPN Gateway
  ✅ GCP HA VPN Gateway
  ✅ BGP 라우팅 설정
  ✅ 연결 테스트

Phase 5: 데이터베이스 복제
  ✅ PostgreSQL Logical Replication
  ✅ 복제 모니터링 설정

Phase 6: 글로벌 LB
  ✅ CloudFlare Load Balancer
  ✅ Health Check 설정
  ✅ DNS 레코드 구성
```

---

## 🧪 테스트 시나리오

### 1. 정상 작동 테스트

```bash
# 전 세계 여러 지역에서 접근 테스트
curl -I https://crm.example.com/health

# 응답 헤더에서 처리한 클라우드 확인
# X-Cloud-Provider: AWS 또는 GCP
```

### 2. AWS 장애 시뮬레이션

```bash
# AWS ALB 헬스체크 실패 시뮬레이션
# (애플리케이션을 일시적으로 중단)

# CloudFlare 대시보드에서 확인:
# - AWS pool이 unhealthy로 변경
# - 트래픽이 100% GCP로 전환
# - 약 30초-2분 소요
```

### 3. 데이터 일관성 테스트

```bash
# AWS에서 데이터 생성
psql -h <rds-endpoint> -U crmadmin -d crm -c \
  "INSERT INTO test_table VALUES (1, 'test');"

# 복제 지연 확인 (일반적으로 1-5초)
sleep 5

# GCP에서 데이터 확인
psql -h <cloudsql-ip> -U crmadmin -d crm -c \
  "SELECT * FROM test_table WHERE id = 1;"
```

---

## 💰 예상 비용 (월간)

### Active-Active 구성

**AWS (50% 트래픽):**
- EKS: $73 + $400 (노드)
- RDS: $300 (db.t3.large)
- ElastiCache: $150
- VPN: $36
- 네트워크: $100
- **소계: ~$1,059/월**

**GCP (50% 트래픽):**
- GKE: $73 + $450 (노드)
- Cloud SQL: $200
- Memorystore: $100
- VPN: $36
- 네트워크: $100
- **소계: ~$959/월**

**글로벌:**
- CloudFlare LB: $50/월
- 데이터 전송: $200/월

**총 예상: ~$2,268/월 (약 295만원)**

> 참고: 실제 비용은 트래픽, 리전, 사용 패턴에 따라 달라집니다.

---

## 📊 성능 지표

### 목표 SLA

- **가용성**: 99.99% (연간 다운타임 < 53분)
- **RTO**: 2-5분 (복구 시간)
- **RPO**: 5초 (데이터 손실)
- **응답 시간**: p95 < 200ms

### 모니터링 대시보드

CloudFlare Analytics에서:
- 트래픽 분산 비율
- Pool health status
- Origin response time
- Failover 이벤트

---

## 🎉 완료!

축하합니다! Active-Active 멀티 클라우드 DR 구성이 완료되었습니다.

**다음 단계:**
1. 애플리케이션 배포 (Kubernetes)
2. 캐시 동기화 로직 구현
3. 정기적인 DR 테스트 (월 1회)
4. 비용 최적화 검토

**문의사항:**
- 각 모듈의 README.md 참고
- Terraform 문서 확인
- CloudFlare 문서 확인

---

**작성:** GitHub Copilot CLI  
**버전:** 1.0  
**업데이트:** 2025년 1월
