# 멀티 클라우드 재해복구(DR) 아키텍처 가이드

## 🎯 목표

**하나의 리전에서 AWS와 GCP를 함께 사용하여, 한 클라우드에 장애가 발생해도 다른 클라우드로 자동/수동 전환하여 서비스 지속**

---

## 🏗️ 필요한 아키텍처 변경사항

### ❌ 현재 문제점: 완전 독립 구성

```
┌─────────────────────────┐     ┌─────────────────────────┐
│   AWS (독립)             │     │   GCP (독립)             │
│                         │     │                         │
│  Application A          │     │  Application B          │
│  Database A (독립)      │     │  Database B (독립)      │
│  Cache A (독립)         │     │  Cache B (독립)         │
│                         │     │                         │
└─────────────────────────┘     └─────────────────────────┘

문제:
❌ 데이터 동기화 없음
❌ AWS 장애 시 GCP가 서비스 불가 (데이터 없음)
❌ 수동으로 데이터 이전 필요
```

### ✅ 필요한 구성: DR 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    글로벌 로드밸런서                          │
│            (CloudFlare, Route53, Traffic Manager)           │
└────────────┬──────────────────────────┬─────────────────────┘
             │                          │
             ▼                          ▼
┌─────────────────────────┐   ┌─────────────────────────┐
│   AWS (Primary)          │   │   GCP (Standby/Active)   │
│                         │   │                         │
│  ┌──────────────────┐  │   │  ┌──────────────────┐  │
│  │  Application     │  │   │  │  Application     │  │
│  │  (Active)        │  │   │  │  (Standby/Active)│  │
│  └────────┬─────────┘  │   │  └────────┬─────────┘  │
│           │             │   │           │             │
│           ▼             │   │           ▼             │
│  ┌──────────────────┐  │   │  ┌──────────────────┐  │
│  │  RDS (Primary)   │──┼───┼─▶│  Cloud SQL       │  │
│  │  ✍️  Read/Write  │  │   │  │  (Replica)       │  │
│  └──────────────────┘  │   │  │  👁️  Read Only   │  │
│                         │   │  └──────────────────┘  │
│  ┌──────────────────┐  │   │  ┌──────────────────┐  │
│  │  ElastiCache     │──┼───┼─▶│  Memorystore     │  │
│  │  (Primary)       │  │   │  │  (Replica)       │  │
│  └──────────────────┘  │   │  └──────────────────┘  │
│                         │   │                         │
└─────────────────────────┘   └─────────────────────────┘
          데이터 복제 ──────────────▶
```

---

## 🔄 DR 전략 비교

### 전략 1: Active-Standby (Warm Standby) ⭐ **권장**

```
정상 시:
  AWS: 100% 트래픽 처리 (Primary)
  GCP: 대기 상태 (데이터만 실시간 복제)

장애 시:
  1. AWS 장애 감지 (헬스체크)
  2. DNS/로드밸런서가 GCP로 트래픽 전환 (자동 5-10분)
  3. GCP에서 서비스 계속

복구 시간 (RTO): 5-15분
데이터 손실 (RPO): 1-5초 (복제 지연)
비용: 중간 (GCP 리소스 대기 중)
```

### 전략 2: Active-Active (Hot Standby)

```
정상 시:
  AWS: 50% 트래픽 처리
  GCP: 50% 트래픽 처리
  양방향 데이터 동기화

장애 시:
  1. AWS 장애 감지
  2. 100% 트래픽이 GCP로 (거의 즉시)
  3. GCP에서 100% 처리

복구 시간 (RTO): 30초-2분
데이터 손실 (RPO): 거의 0
비용: 높음 (두 클라우드 모두 풀 가동)
```

### 전략 3: Pilot Light (Cold Standby)

```
정상 시:
  AWS: 100% 트래픽
  GCP: 최소한의 리소스만 (DB 복제만)

장애 시:
  1. 수동으로 GCP 리소스 프로비저닝
  2. 애플리케이션 배포
  3. 트래픽 전환

복구 시간 (RTO): 30분-2시간
데이터 손실 (RPO): 5-30초
비용: 낮음 (최소 리소스)
```

---

## 🏗️ Active-Standby DR 구성 (권장)

### 1. 글로벌 로드밸런서 설정

#### CloudFlare Load Balancer (권장)

```hcl
# terraform/modules/cloudflare-lb/main.tf
resource "cloudflare_load_balancer_pool" "aws_pool" {
  name = "crm-aws-pool"
  
  origins {
    name    = "aws-origin"
    address = aws_lb.main.dns_name  # AWS ALB
    enabled = true
  }
  
  # 헬스체크
  monitor = cloudflare_load_balancer_monitor.health_check.id
}

resource "cloudflare_load_balancer_pool" "gcp_pool" {
  name = "crm-gcp-pool"
  
  origins {
    name    = "gcp-origin"
    address = google_compute_global_address.lb.address  # GCP LB
    enabled = true
  }
  
  monitor = cloudflare_load_balancer_monitor.health_check.id
}

resource "cloudflare_load_balancer" "main" {
  zone_id = var.cloudflare_zone_id
  name    = "crm.example.com"
  
  default_pool_ids = [
    cloudflare_load_balancer_pool.aws_pool.id
  ]
  
  fallback_pool_id = cloudflare_load_balancer_pool.gcp_pool.id
  
  # 장애 감지 시 자동 전환
  steering_policy = "dynamic_latency"
  
  # 세션 어피니티
  session_affinity = "cookie"
}

resource "cloudflare_load_balancer_monitor" "health_check" {
  type     = "https"
  port     = 443
  path     = "/health"
  interval = 60
  retries  = 2
  timeout  = 5
}
```

#### AWS Route53 대안

```hcl
# terraform/modules/route53-failover/main.tf
resource "aws_route53_health_check" "aws_health" {
  fqdn              = aws_lb.main.dns_name
  port              = 443
  type              = "HTTPS"
  resource_path     = "/health"
  failure_threshold = "3"
  request_interval  = "30"
}

resource "aws_route53_record" "primary" {
  zone_id = var.route53_zone_id
  name    = "crm.example.com"
  type    = "A"
  
  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
  
  set_identifier = "aws-primary"
  failover_routing_policy {
    type = "PRIMARY"
  }
  
  health_check_id = aws_route53_health_check.aws_health.id
}

resource "aws_route53_record" "secondary" {
  zone_id = var.route53_zone_id
  name    = "crm.example.com"
  type    = "A"
  
  alias {
    name                   = google_compute_global_address.lb.address
    zone_id                = var.gcp_lb_zone_id
    evaluate_target_health = false
  }
  
  set_identifier = "gcp-secondary"
  failover_routing_policy {
    type = "SECONDARY"
  }
}
```

---

### 2. 데이터베이스 복제 설정

#### 방법 1: AWS DMS를 사용한 복제 (권장)

```hcl
# terraform/modules/dms-replication/main.tf

# VPN 또는 VPC Peering 필요
resource "aws_dms_replication_subnet_group" "this" {
  replication_subnet_group_id          = "crm-dms-subnet-group"
  replication_subnet_group_description = "DMS subnet group"
  subnet_ids                           = var.private_subnet_ids
}

resource "aws_dms_replication_instance" "this" {
  replication_instance_id   = "crm-dms-replication"
  replication_instance_class = "dms.c5.xlarge"
  allocated_storage         = 100
  vpc_security_group_ids    = [aws_security_group.dms.id]
  replication_subnet_group_id = aws_dms_replication_subnet_group.this.id
  
  publicly_accessible = false
  multi_az           = true
  
  tags = var.tags
}

# Source: AWS RDS
resource "aws_dms_endpoint" "source" {
  endpoint_id   = "crm-rds-source"
  endpoint_type = "source"
  engine_name   = "postgres"
  
  server_name = module.rds.address
  port        = 5432
  database_name = var.database_name
  username    = var.rds_username
  password    = var.rds_password
  
  ssl_mode = "require"
}

# Target: GCP Cloud SQL (VPN 또는 Public IP 필요)
resource "aws_dms_endpoint" "target" {
  endpoint_id   = "crm-cloudsql-target"
  endpoint_type = "target"
  engine_name   = "postgres"
  
  server_name = module.cloud_sql.public_ip_address  # 또는 VPN을 통한 Private IP
  port        = 5432
  database_name = var.database_name
  username    = var.cloudsql_username
  password    = var.cloudsql_password
  
  ssl_mode = "require"
}

# 복제 작업
resource "aws_dms_replication_task" "this" {
  replication_task_id       = "crm-db-replication"
  migration_type            = "cdc"  # Change Data Capture (실시간 복제)
  replication_instance_arn  = aws_dms_replication_instance.this.replication_instance_arn
  source_endpoint_arn       = aws_dms_endpoint.source.endpoint_arn
  target_endpoint_arn       = aws_dms_endpoint.target.endpoint_arn
  table_mappings           = jsonencode({
    rules = [
      {
        rule-type = "selection"
        rule-id   = "1"
        rule-name = "1"
        object-locator = {
          schema-name = "public"
          table-name  = "%"
        }
        rule-action = "include"
      }
    ]
  })
  
  replication_task_settings = jsonencode({
    TargetMetadata = {
      TargetSchema = ""
      SupportLobs = true
      FullLobMode = false
      LobChunkSize = 64
      LimitedSizeLobMode = true
      LobMaxSize = 32
    }
    FullLoadSettings = {
      TargetTablePrepMode = "DROP_AND_CREATE"
    }
    Logging = {
      EnableLogging = true
      LogComponents = [
        {
          Id = "TRANSFORMATION"
          Severity = "LOGGER_SEVERITY_DEFAULT"
        },
        {
          Id = "SOURCE_CAPTURE"
          Severity = "LOGGER_SEVERITY_DEFAULT"
        },
        {
          Id = "TARGET_APPLY"
          Severity = "LOGGER_SEVERITY_DEFAULT"
        }
      ]
    }
  })
  
  tags = var.tags
}
```

#### 방법 2: PostgreSQL Native Logical Replication

```hcl
# terraform/environments/aws/production/main.tf
# RDS에서 Logical Replication 활성화
module "rds" {
  source = "../../modules/rds"
  
  # ... 기존 설정 ...
  
  parameters = [
    {
      name  = "rds.logical_replication"
      value = "1"
    },
    {
      name  = "wal_sender_timeout"
      value = "0"
    }
  ]
}
```

```sql
-- AWS RDS에서 실행
-- 1. Publication 생성
CREATE PUBLICATION crm_replication FOR ALL TABLES;

-- 2. Replication 슬롯 생성
SELECT pg_create_logical_replication_slot('crm_slot', 'pgoutput');

-- GCP Cloud SQL에서 실행
-- 3. Subscription 생성 (VPN 필요)
CREATE SUBSCRIPTION crm_subscription
CONNECTION 'host=aws-rds-endpoint.rds.amazonaws.com port=5432 dbname=crm user=replication_user password=xxx sslmode=require'
PUBLICATION crm_replication
WITH (slot_name = 'crm_slot');
```

---

### 3. 네트워크 연결 (VPN 또는 Interconnect)

#### AWS-GCP VPN 연결

```hcl
# terraform/modules/aws-gcp-vpn/main.tf

# AWS측 설정
resource "aws_vpn_gateway" "main" {
  vpc_id = var.aws_vpc_id
  
  tags = {
    Name = "crm-aws-vpn-gw"
  }
}

resource "aws_customer_gateway" "gcp" {
  bgp_asn    = 65000
  ip_address = google_compute_ha_vpn_gateway.main.vpn_interfaces[0].ip_address
  type       = "ipsec.1"
  
  tags = {
    Name = "crm-gcp-customer-gw"
  }
}

resource "aws_vpn_connection" "main" {
  vpn_gateway_id      = aws_vpn_gateway.main.id
  customer_gateway_id = aws_customer_gateway.gcp.id
  type                = "ipsec.1"
  static_routes_only  = false
  
  tags = {
    Name = "crm-aws-gcp-vpn"
  }
}

# GCP측 설정
resource "google_compute_ha_vpn_gateway" "main" {
  name    = "crm-gcp-vpn-gw"
  network = var.gcp_network_id
  region  = var.gcp_region
}

resource "google_compute_external_vpn_gateway" "aws" {
  name            = "aws-peer-gateway"
  redundancy_type = "SINGLE_IP_INTERNALLY_REDUNDANT"
  
  interface {
    id         = 0
    ip_address = aws_vpn_connection.main.tunnel1_address
  }
}

resource "google_compute_vpn_tunnel" "tunnel1" {
  name                  = "crm-tunnel-1"
  peer_external_gateway = google_compute_external_vpn_gateway.aws.id
  peer_external_gateway_interface = 0
  shared_secret        = aws_vpn_connection.main.tunnel1_preshared_key
  ike_version         = 2
  vpn_gateway         = google_compute_ha_vpn_gateway.main.id
  vpn_gateway_interface = 0
}

# BGP 라우팅
resource "google_compute_router" "router" {
  name    = "crm-router"
  network = var.gcp_network_id
  region  = var.gcp_region
  
  bgp {
    asn = 65000
  }
}

resource "google_compute_router_peer" "aws_peer" {
  name                      = "aws-peer"
  router                    = google_compute_router.router.name
  region                    = var.gcp_region
  peer_ip_address          = aws_vpn_connection.main.tunnel1_cgw_inside_address
  peer_asn                 = 64512
  advertised_route_priority = 100
  interface                = google_compute_router_interface.interface1.name
}
```

---

### 4. 애플리케이션 배포 (양쪽 클라우드)

#### Kubernetes 멀티 클러스터 관리

```yaml
# k8s/base/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: crm-app
  labels:
    app: crm
spec:
  replicas: 3
  selector:
    matchLabels:
      app: crm
  template:
    metadata:
      labels:
        app: crm
    spec:
      containers:
      - name: crm
        image: ${CONTAINER_REGISTRY}/crm-app:${VERSION}
        env:
        # 환경별 데이터베이스 설정
        - name: DATABASE_HOST
          valueFrom:
            configMapKeyRef:
              name: db-config
              key: host
        - name: DATABASE_MODE
          value: "read-write"  # AWS는 read-write, GCP는 read-only
        - name: CACHE_HOST
          valueFrom:
            configMapKeyRef:
              name: cache-config
              key: host
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

```yaml
# k8s/overlays/aws/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

bases:
  - ../../base

configMapGenerator:
  - name: db-config
    literals:
      - host=aws-rds-endpoint.rds.amazonaws.com
      - port=5432
      - mode=primary
  - name: cache-config
    literals:
      - host=aws-elasticache-endpoint
      - port=6379

images:
  - name: ${CONTAINER_REGISTRY}/crm-app
    newName: ${AWS_ECR_URL}/crm-app
    newTag: ${VERSION}
```

```yaml
# k8s/overlays/gcp/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

bases:
  - ../../base

configMapGenerator:
  - name: db-config
    literals:
      - host=gcp-cloudsql-ip
      - port=5432
      - mode=replica  # Standby 모드
  - name: cache-config
    literals:
      - host=gcp-memorystore-host
      - port=6379

images:
  - name: ${CONTAINER_REGISTRY}/crm-app
    newName: ${GCP_ARTIFACT_REGISTRY_URL}/crm-app
    newTag: ${VERSION}

replicas:
  - name: crm-app
    count: 1  # Standby는 최소한의 리소스
```

---

### 5. 캐시 동기화 (Redis)

```python
# app/cache_sync.py
import redis
from redis.sentinel import Sentinel

class MultiCloudCache:
    def __init__(self):
        # AWS ElastiCache (Primary)
        self.aws_cache = redis.Redis(
            host=os.getenv('AWS_REDIS_HOST'),
            port=6379,
            password=os.getenv('AWS_REDIS_PASSWORD'),
            ssl=True
        )
        
        # GCP Memorystore (Replica)
        self.gcp_cache = redis.Redis(
            host=os.getenv('GCP_REDIS_HOST'),
            port=6379,
            password=os.getenv('GCP_REDIS_PASSWORD'),
            ssl=True
        )
        
        self.is_primary = os.getenv('CLOUD_ROLE') == 'primary'
    
    def set(self, key, value, ttl=3600):
        """캐시 설정 (양쪽에 동시 저장)"""
        try:
            if self.is_primary:
                self.aws_cache.setex(key, ttl, value)
            # 백그라운드로 복제
            self._replicate_async(key, value, ttl)
        except Exception as e:
            logger.error(f"Cache set error: {e}")
    
    def get(self, key):
        """캐시 조회 (로컬 캐시 우선)"""
        try:
            if self.is_primary:
                return self.aws_cache.get(key)
            else:
                # GCP에서는 로컬 캐시 사용
                value = self.gcp_cache.get(key)
                if not value:
                    # 캐시 미스 시 DB에서 조회
                    value = self._load_from_db(key)
                return value
        except Exception as e:
            logger.error(f"Cache get error: {e}")
            return None
```

또는 **Redis Streams를 사용한 동기화**:

```python
# AWS에서 변경사항을 Stream에 기록
aws_cache.xadd('cache_updates', {
    'key': key,
    'value': value,
    'ttl': ttl,
    'timestamp': time.time()
})

# GCP에서 Stream을 읽어서 반영 (비동기)
async def sync_cache_from_stream():
    while True:
        messages = gcp_cache.xread({'cache_updates': last_id}, block=1000)
        for stream, msgs in messages:
            for msg_id, data in msgs:
                gcp_cache.setex(data['key'], int(data['ttl']), data['value'])
                last_id = msg_id
```

---

### 6. 모니터링 및 자동 장애조치

```hcl
# terraform/modules/monitoring/main.tf

# CloudWatch Alarms (AWS 헬스체크)
resource "aws_cloudwatch_metric_alarm" "app_health" {
  alarm_name          = "crm-app-health-check"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "HealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Average"
  threshold           = "1"
  alarm_description   = "Alert when app is unhealthy"
  
  alarm_actions = [var.sns_topic_arn]
  
  dimensions = {
    LoadBalancer = aws_lb.main.arn_suffix
    TargetGroup  = aws_lb_target_group.app.arn_suffix
  }
}

# GCP Monitoring (장애 감지)
resource "google_monitoring_alert_policy" "app_health" {
  display_name = "CRM App Health Check"
  combiner     = "OR"
  
  conditions {
    display_name = "App Down"
    
    condition_threshold {
      filter          = "resource.type=\"k8s_container\" AND metric.type=\"kubernetes.io/container/uptime\""
      duration        = "60s"
      comparison      = "COMPARISON_LT"
      threshold_value = 1
    }
  }
  
  notification_channels = [google_monitoring_notification_channel.email.id]
  
  alert_strategy {
    auto_close = "1800s"
  }
}
```

---

## 📋 체크리스트

### Phase 1: 기반 구성
- [ ] AWS-GCP VPN 연결 설정
- [ ] 네트워크 라우팅 구성
- [ ] 보안 그룹 / 방화벽 규칙 설정

### Phase 2: 데이터베이스 복제
- [ ] AWS RDS Logical Replication 활성화
- [ ] GCP Cloud SQL 복제 설정
- [ ] DMS 복제 작업 구성 및 테스트
- [ ] 복제 지연 모니터링 설정

### Phase 3: 애플리케이션 배포
- [ ] 양쪽 클라우드에 동일 애플리케이션 배포
- [ ] 환경별 설정 구성 (Primary/Standby)
- [ ] 컨테이너 레지스트리 복제 설정

### Phase 4: 로드밸런싱
- [ ] CloudFlare/Route53 글로벌 LB 설정
- [ ] 헬스체크 구성
- [ ] 장애조치 정책 설정

### Phase 5: 모니터링
- [ ] 양쪽 클라우드 모니터링 대시보드
- [ ] 알람 및 알림 설정
- [ ] 복제 지연 모니터링
- [ ] 로그 중앙화

### Phase 6: 테스트
- [ ] 장애조치 시나리오 테스트
- [ ] 데이터 일관성 검증
- [ ] 성능 테스트
- [ ] 롤백 절차 테스트

---

## 💰 예상 비용

### Warm Standby 구성 (월간)

**AWS (Primary):**
- EKS: $73 (클러스터) + $200 (노드 t3.xlarge × 3)
- RDS: $150 (db.t3.medium)
- ElastiCache: $80 (cache.t3.small)
- DMS: $200 (복제 인스턴스)
- 네트워크: $50
- **소계: ~$753/월**

**GCP (Standby):**
- GKE: $73 (클러스터) + $100 (노드 e2-standard-4 × 1-2, 최소)
- Cloud SQL: $75 (db-custom-2-7680, replica)
- Memorystore: $40 (BASIC 1GB)
- 네트워크: $30
- **소계: ~$318/월**

**기타:**
- VPN: $50/월
- CloudFlare LB: $50/월
- 데이터 전송: $100/월

**총 예상 비용: ~$1,271/월 (약 165만원)**

> 비교: 단일 클라우드 HA 구성 시 약 $800/월

---

## ⚡ 다음 단계

1. **VPN 구성부터 시작**
2. **데이터베이스 복제 설정**
3. **애플리케이션 배포 테스트**
4. **글로벌 LB 구성**
5. **장애조치 시나리오 테스트**

상세한 구현 코드는 다음 단계에서 제공하겠습니다!

---

**작성:** GitHub Copilot CLI  
**버전:** 1.0  
**업데이트:** 2025년 1월
