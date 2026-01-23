# GCP Memorystore (Redis) Terraform Module

이 모듈은 GCP Memorystore for Redis 인스턴스를 생성하고 관리합니다.

## 기능

- ✅ Redis 인스턴스 생성 (BASIC 또는 STANDARD_HA 티어)
- ✅ 고가용성(HA) 구성 지원
- ✅ Read Replicas 지원
- ✅ AUTH 인증 활성화
- ✅ 전송 중 암호화 (TLS)
- ✅ 데이터 영속성 (RDB 스냅샷)
- ✅ 자동 유지보수 스케줄링
- ✅ VPC 네트워크 통합
- ✅ 커스텀 Redis 설정

## 사용 예시

### 기본 구성 (고가용성)

```hcl
module "redis" {
  source = "../../modules/memorystore"

  instance_id        = "crm-redis-dev"
  display_name       = "CRM Redis Development"
  region             = "us-central1"
  tier               = "STANDARD_HA"
  memory_size_gb     = 2
  redis_version      = "REDIS_7_0"
  
  # Network
  authorized_network = "projects/my-project/global/networks/my-vpc"
  connect_mode       = "DIRECT_PEERING"
  
  # Security
  auth_enabled              = true
  transit_encryption_mode   = "SERVER_AUTHENTICATION"
  
  # Persistence
  persistence_mode     = "RDB"
  rdb_snapshot_period  = "TWELVE_HOURS"
  
  labels = {
    environment = "dev"
    application = "crm"
    managed_by  = "terraform"
  }
}
```

### 고성능 구성 (Read Replicas)

```hcl
module "redis_ha" {
  source = "../../modules/memorystore"

  instance_id        = "crm-redis-prod"
  display_name       = "CRM Redis Production"
  region             = "us-central1"
  tier               = "STANDARD_HA"
  memory_size_gb     = 10
  redis_version      = "REDIS_7_0"
  
  # High Availability
  replica_count       = 2
  read_replicas_mode  = "READ_REPLICAS_ENABLED"
  
  # Network
  authorized_network = "projects/my-project/global/networks/my-vpc"
  connect_mode       = "PRIVATE_SERVICE_ACCESS"
  reserved_ip_range  = "10.20.10.0/29"
  
  # Security
  auth_enabled              = true
  transit_encryption_mode   = "SERVER_AUTHENTICATION"
  
  # Redis Configuration
  redis_configs = {
    maxmemory-policy = "allkeys-lru"
    notify-keyspace-events = "Ex"
  }
  
  # Maintenance
  maintenance_day          = "SUN"
  maintenance_start_hour   = 4
  maintenance_start_minute = 0
  
  # Persistence
  persistence_mode     = "RDB"
  rdb_snapshot_period  = "SIX_HOURS"
  
  # Protection
  prevent_destroy = true
  
  labels = {
    environment = "prod"
    application = "crm"
    managed_by  = "terraform"
    tier        = "critical"
  }
}
```

### 개발 환경 (BASIC 티어)

```hcl
module "redis_dev" {
  source = "../../modules/memorystore"

  instance_id        = "crm-redis-dev"
  region             = "us-central1"
  tier               = "BASIC"
  memory_size_gb     = 1
  redis_version      = "REDIS_7_0"
  
  # Network
  authorized_network = "projects/my-project/global/networks/my-vpc"
  
  # Security (minimal for dev)
  auth_enabled              = false
  transit_encryption_mode   = "DISABLED"
  
  # No persistence for dev
  persistence_mode = "DISABLED"
  
  labels = {
    environment = "dev"
  }
}
```

## Requirements

| Name | Version |
|------|---------|
| terraform | >= 1.5.0 |
| google | >= 5.0 |

## Providers

| Name | Version |
|------|---------|
| google | >= 5.0 |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| instance_id | Unique identifier for the Redis instance | `string` | n/a | yes |
| region | GCP region for the Redis instance | `string` | n/a | yes |
| authorized_network | VPC network to attach the Redis instance to | `string` | n/a | yes |
| display_name | Display name for the Redis instance | `string` | `null` | no |
| tier | Service tier (BASIC or STANDARD_HA) | `string` | `"STANDARD_HA"` | no |
| memory_size_gb | Redis memory size in GiB (1-300) | `number` | `1` | no |
| redis_version | Redis version (REDIS_7_0, REDIS_6_X, REDIS_5_0) | `string` | `"REDIS_7_0"` | no |
| replica_count | Number of replica nodes (0-5) | `number` | `1` | no |
| read_replicas_mode | Read replicas mode | `string` | `"READ_REPLICAS_DISABLED"` | no |
| connect_mode | Connection mode | `string` | `"DIRECT_PEERING"` | no |
| reserved_ip_range | CIDR range for internal addresses | `string` | `null` | no |
| auth_enabled | Enable Redis AUTH | `bool` | `true` | no |
| transit_encryption_mode | TLS mode | `string` | `"SERVER_AUTHENTICATION"` | no |
| redis_configs | Redis configuration parameters | `map(string)` | `{}` | no |
| maintenance_day | Day of week for maintenance | `string` | `"SUN"` | no |
| maintenance_start_hour | Hour for maintenance start (0-23) | `number` | `4` | no |
| maintenance_start_minute | Minute for maintenance start (0-59) | `number` | `0` | no |
| persistence_mode | Persistence mode (DISABLED or RDB) | `string` | `"RDB"` | no |
| rdb_snapshot_period | RDB snapshot period | `string` | `"TWELVE_HOURS"` | no |
| prevent_destroy | Prevent Terraform from destroying instance | `bool` | `false` | no |
| labels | Labels to apply to the instance | `map(string)` | `{}` | no |

## Outputs

| Name | Description |
|------|-------------|
| id | Identifier of the Redis instance |
| name | Name of the Redis instance |
| host | Hostname or IP address of the Redis instance |
| port | Port number of the Redis instance |
| current_location_id | Current zone where endpoint is placed |
| auth_string | AUTH string for Redis (sensitive) |
| connection_string | Redis connection string (host:port) |
| read_endpoint | Read endpoint for read replicas |
| read_endpoint_port | Read endpoint port |
| server_ca_certs | Server CA certificates for TLS (sensitive) |

## Redis 버전

- `REDIS_7_0` - Redis 7.0 (권장)
- `REDIS_6_X` - Redis 6.x
- `REDIS_5_0` - Redis 5.0

## 티어 비교

### BASIC
- 단일 인스턴스
- 복제본 없음
- SLA 없음
- 저렴한 비용
- 개발/테스트 환경에 적합

### STANDARD_HA
- 고가용성 (복제본 자동 구성)
- 자동 장애조치
- 99.9% SLA
- Read Replicas 지원
- 프로덕션 환경에 적합

## 보안 고려사항

1. **인증 활성화**: 프로덕션 환경에서는 반드시 `auth_enabled = true` 설정
2. **전송 암호화**: TLS 활성화 (`transit_encryption_mode = "SERVER_AUTHENTICATION"`)
3. **네트워크 격리**: VPC 네트워크를 통한 프라이빗 액세스
4. **IP 제한**: `authorized_network`를 통한 접근 제어

## 성능 튜닝

```hcl
redis_configs = {
  # 메모리 정책
  maxmemory-policy = "allkeys-lru"  # LRU 삭제 정책
  
  # 키스페이스 알림
  notify-keyspace-events = "Ex"     # 만료 이벤트 알림
  
  # 타임아웃
  timeout = "300"                   # 클라이언트 타임아웃 (초)
  
  # 영속성
  activedefrag = "yes"              # 메모리 조각 모음
}
```

## 비용 최적화

1. **적절한 티어 선택**: 개발은 BASIC, 프로덕션은 STANDARD_HA
2. **메모리 크기**: 필요한 만큼만 할당 (모니터링 후 조정)
3. **리전 선택**: 애플리케이션과 동일한 리전 사용
4. **Read Replicas**: 읽기 부하가 높을 때만 활성화

## 연결 방법

### Kubernetes에서 연결

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: redis-config
data:
  REDIS_HOST: "10.0.0.3"  # module output: host
  REDIS_PORT: "6379"       # module output: port
---
apiVersion: v1
kind: Secret
metadata:
  name: redis-auth
type: Opaque
stringData:
  AUTH_STRING: "<auth_string_from_output>"
```

### 애플리케이션 코드

```python
import redis

r = redis.Redis(
    host='10.0.0.3',
    port=6379,
    password='<auth_string>',
    ssl=True,
    ssl_cert_reqs='required'
)
```

## 모니터링

Cloud Console에서 다음 메트릭 모니터링:
- CPU 사용률
- 메모리 사용률
- 연결 수
- 캐시 히트율
- 네트워크 처리량

## 참고 자료

- [GCP Memorystore Documentation](https://cloud.google.com/memorystore/docs/redis)
- [Redis Best Practices](https://cloud.google.com/memorystore/docs/redis/redis-best-practices)
- [Terraform Google Provider](https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/redis_instance)

## 라이선스

이 모듈은 프로젝트 라이선스를 따릅니다.
