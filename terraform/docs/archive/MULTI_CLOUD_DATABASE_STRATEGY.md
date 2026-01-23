# 멀티 클라우드 데이터베이스 전략 가이드

## 🤔 현재 구성 설명

### 현재 상태: 독립적인 데이터베이스

```
┌─────────────────────────────┐     ┌─────────────────────────────┐
│        AWS 환경              │     │        GCP 환경              │
│                             │     │                             │
│  ┌──────────────────────┐  │     │  ┌──────────────────────┐  │
│  │   EKS Cluster        │  │     │  │   GKE Cluster        │  │
│  │   (Application)      │  │     │  │   (Application)      │  │
│  └──────────┬───────────┘  │     │  └──────────┬───────────┘  │
│             │               │     │             │               │
│             ▼               │     │             ▼               │
│  ┌──────────────────────┐  │     │  ┌──────────────────────┐  │
│  │   AWS RDS            │  │     │  │   Cloud SQL          │  │
│  │   (PostgreSQL)       │  │     │  │   (PostgreSQL)       │  │
│  │   - 독립적인 데이터   │  │     │  │   - 독립적인 데이터   │  │
│  └──────────────────────┘  │     │  └──────────────────────┘  │
│                             │     │                             │
└─────────────────────────────┘     └─────────────────────────────┘
     ❌ 복제 없음                          ❌ 복제 없음
```

**핵심**: AWS RDS와 GCP Cloud SQL은 **서로 통신하지 않으며**, 각각 독립적인 데이터를 저장합니다.

---

## 📊 멀티 클라우드 데이터베이스 전략

### 전략 1: 독립 환경 (현재 구성) ✅ 권장

**각 클라우드에서 완전히 독립적인 환경 운영**

```
AWS 환경 (완전 독립)          GCP 환경 (완전 독립)
├── Application              ├── Application
├── Database                 ├── Database
└── Cache                    └── Cache

사용 사례:
- 리전별 독립 서비스 (한국/일본)
- 클라우드별 다른 서비스/고객
- 재해 복구 시나리오 테스트
```

**장점:**
- ✅ 간단한 관리
- ✅ 클라우드 장애 시 완전 독립적 운영
- ✅ 비용 효율적
- ✅ 데이터 주권 준수 용이
- ✅ 설정 및 관리 복잡도 최소

**단점:**
- ❌ 데이터 동기화 없음
- ❌ 데이터 이중 관리

**적합한 경우:**
- 리전별 완전히 다른 데이터 (한국 고객 vs 일본 고객)
- 개발/스테이징 환경 분리
- DR(재해복구) 목적의 백업 환경

---

### 전략 2: Primary-Replica (단방향 복제)

**하나의 Primary DB에서 다른 클라우드로 Read Replica 구성**

```
┌─────────────────────────────┐     ┌─────────────────────────────┐
│   AWS (Primary)              │     │   GCP (Replica)              │
│                             │     │                             │
│  ┌──────────────────────┐  │     │  ┌──────────────────────┐  │
│  │   Application        │  │     │  │   Application        │  │
│  │   (Read/Write)       │  │     │  │   (Read Only)        │  │
│  └──────────┬───────────┘  │     │  └──────────┬───────────┘  │
│             │               │     │             │               │
│             ▼               │     │             ▼               │
│  ┌──────────────────────┐  │     │  ┌──────────────────────┐  │
│  │   RDS (Primary)      │──┼────▶│  │   Cloud SQL (Replica)│  │
│  │   ✍️  Write          │  │     │  │   👁️  Read Only      │  │
│  └──────────────────────┘  │     │  └──────────────────────┘  │
│                             │     │                             │
└─────────────────────────────┘     └─────────────────────────────┘
            복제 ────────────────▶
```

**구현 방법:**

1. **AWS DMS (Database Migration Service) 사용**
   ```hcl
   # AWS에서 GCP로 지속적 복제
   resource "aws_dms_replication_instance" "this" {
     replication_instance_id   = "crm-replication"
     replication_instance_class = "dms.t3.medium"
   }
   
   resource "aws_dms_endpoint" "source" {
     endpoint_type = "source"
     engine_name   = "postgres"
     # AWS RDS 연결 정보
   }
   
   resource "aws_dms_endpoint" "target" {
     endpoint_type = "target"
     engine_name   = "postgres"
     # GCP Cloud SQL 연결 정보 (Public IP 또는 VPN 필요)
   }
   ```

2. **PostgreSQL Logical Replication (Native)**
   ```sql
   -- AWS RDS (Primary)
   ALTER SYSTEM SET wal_level = logical;
   CREATE PUBLICATION crm_pub FOR ALL TABLES;
   
   -- GCP Cloud SQL (Replica)
   CREATE SUBSCRIPTION crm_sub 
   CONNECTION 'host=aws-rds-endpoint port=5432 dbname=crm'
   PUBLICATION crm_pub;
   ```

**장점:**
- ✅ 읽기 부하 분산
- ✅ 재해 복구 준비
- ✅ 지역별 읽기 성능 향상

**단점:**
- ⚠️ 복제 지연 (수초~수분)
- ⚠️ 추가 비용 (네트워크 전송)
- ⚠️ 복잡한 설정 및 관리
- ⚠️ VPN 또는 Public IP 필요

---

### 전략 3: Active-Active (양방향 복제)

**두 클라우드 모두에서 읽기/쓰기 가능**

```
┌─────────────────────────────┐     ┌─────────────────────────────┐
│   AWS (Active)               │     │   GCP (Active)               │
│                             │     │                             │
│  ┌──────────────────────┐  │     │  ┌──────────────────────┐  │
│  │   Application        │  │     │  │   Application        │  │
│  │   (Read/Write)       │  │     │  │   (Read/Write)       │  │
│  └──────────┬───────────┘  │     │  └──────────┬───────────┘  │
│             │               │     │             │               │
│             ▼               │     │             ▼               │
│  ┌──────────────────────┐  │     │  ┌──────────────────────┐  │
│  │   RDS                │◀─┼─────┼─▶│   Cloud SQL          │  │
│  │   ✍️  Read/Write     │  │     │  │   ✍️  Read/Write     │  │
│  └──────────────────────┘  │     │  └──────────────────────┘  │
│                             │     │                             │
└─────────────────────────────┘     └─────────────────────────────┘
        양방향 복제 ◀────────────────▶
```

**구현 방법:**

1. **써드파티 솔루션 사용**
   - **Bucardo** (PostgreSQL 전용)
   - **SymmetricDS** (범용)
   - **Debezium + Kafka** (이벤트 기반)

2. **애플리케이션 레벨 동기화**
   ```
   애플리케이션 → 메시지 큐 → 양쪽 DB 업데이트
   ```

**장점:**
- ✅ 지역별 낮은 지연시간
- ✅ 고가용성 (한쪽 장애 시 다른 쪽 사용)
- ✅ 글로벌 서비스 최적화

**단점:**
- ❌ 매우 복잡한 구성
- ❌ 충돌 해결 메커니즘 필요
- ❌ 높은 비용
- ❌ 데이터 일관성 보장 어려움

---

### 전략 4: 글로벌 데이터베이스 서비스

**클라우드 네이티브 글로벌 DB 사용**

```
┌─────────────────────────────┐     ┌─────────────────────────────┐
│   AWS                        │     │   GCP                        │
│                             │     │                             │
│  ┌──────────────────────┐  │     │  ┌──────────────────────┐  │
│  │   Application        │  │     │  │   Application        │  │
│  └──────────┬───────────┘  │     │  └──────────┬───────────┘  │
│             │               │     │             │               │
│             └───────────────┼─────┼──────────────┘              │
│                             │     │                             │
└─────────────────────────────┘     └─────────────────────────────┘
                                │
                                ▼
                    ┌────────────────────────┐
                    │  CockroachDB Cloud     │
                    │  MongoDB Atlas         │
                    │  YugabyteDB Cloud      │
                    └────────────────────────┘
                      (Multi-Cloud Native)
```

**옵션:**
- **CockroachDB Cloud** - PostgreSQL 호환
- **MongoDB Atlas** - NoSQL
- **YugabyteDB Cloud** - PostgreSQL 호환
- **Google Cloud Spanner** - 글로벌 분산

**장점:**
- ✅ 클라우드 중립적
- ✅ 자동 복제 및 장애조치
- ✅ 글로벌 일관성
- ✅ 관리 간소화

**단점:**
- ⚠️ 벤더 종속성 (다른 벤더로)
- ⚠️ 추가 비용
- ⚠️ 기존 PostgreSQL에서 마이그레이션 필요

---

## 💡 권장 전략

### 현재 CRM 프로젝트에 대한 권장사항:

#### 📍 시나리오별 전략

**시나리오 1: 리전별 독립 서비스** (현재 구성 유지) ⭐ **권장**
```
- AWS: 한국 고객 서비스
- GCP: 일본 고객 서비스
- 전략: 독립 환경
- 이유: 데이터 주권, 간단한 관리, 비용 효율
```

**시나리오 2: DR(재해복구) 목적**
```
- AWS: Primary (프로덕션)
- GCP: Replica (읽기 전용 백업)
- 전략: Primary-Replica
- 이유: 비용 vs 복원력 균형
```

**시나리오 3: 글로벌 서비스**
```
- 전략: 글로벌 데이터베이스 서비스
- 솔루션: CockroachDB Cloud 또는 YugabyteDB
- 이유: 전 세계 낮은 지연시간, 자동 복제
```

---

## 🔧 구현 가이드

### 옵션 1: 현재 구성 유지 (독립 환경)

**그대로 사용하면 됩니다!** 각 클라우드는 독립적입니다.

```bash
# AWS 배포
cd terraform/environments/aws/production
terraform apply

# GCP 배포  
cd terraform/environments/gcp/production
terraform apply
```

**애플리케이션 구성:**
```yaml
# AWS EKS
apiVersion: v1
kind: ConfigMap
metadata:
  name: db-config
data:
  DATABASE_HOST: "aws-rds-endpoint.region.rds.amazonaws.com"
  DATABASE_PORT: "5432"
```

```yaml
# GCP GKE
apiVersion: v1
kind: ConfigMap
metadata:
  name: db-config
data:
  DATABASE_HOST: "gcp-cloudsql-ip"
  DATABASE_PORT: "5432"
```

---

### 옵션 2: Primary-Replica 구현

#### 1단계: VPN 또는 Cloud Interconnect 설정

```hcl
# AWS-GCP VPN 연결
module "aws_vpn" {
  source = "terraform-aws-modules/vpn-gateway/aws"
  
  vpc_id              = module.networking.vpc_id
  customer_gateway_ip = var.gcp_vpn_gateway_ip
}
```

#### 2단계: RDS를 Primary로 설정

```hcl
# AWS RDS - Primary
module "rds" {
  source = "../../modules/rds"
  
  # Logical Replication 활성화
  parameters = [
    {
      name  = "rds.logical_replication"
      value = "1"
    }
  ]
}
```

#### 3단계: Cloud SQL을 Replica로 설정

```sql
-- GCP Cloud SQL에서 실행
CREATE SUBSCRIPTION crm_replica 
CONNECTION 'host=aws-rds-endpoint.com port=5432 dbname=crm user=repl_user password=xxx'
PUBLICATION crm_pub;
```

#### 4단계: 애플리케이션 수정

```python
# Read/Write 분리
class DatabaseRouter:
    def db_for_read(self, model, **hints):
        # GCP에서는 로컬 Cloud SQL 읽기
        if is_gcp_environment():
            return 'replica'
        return 'primary'
    
    def db_for_write(self, model, **hints):
        # 모든 쓰기는 AWS RDS로
        return 'primary'
```

---

### 옵션 3: 글로벌 데이터베이스 마이그레이션

```hcl
# CockroachDB Cloud 사용
resource "cockroach_cluster" "crm" {
  name           = "crm-global"
  cloud_provider = "AWS"  # 또는 "GCP"
  regions = [
    {
      name = "us-west-2"  # AWS
    },
    {
      name = "asia-northeast1"  # GCP
    }
  ]
}
```

---

## 📊 비교표

| 전략 | 설정 복잡도 | 비용 | 데이터 일관성 | HA | 적합한 용도 |
|------|-----------|------|--------------|-----|-----------|
| **독립 환경** | ⭐ 낮음 | 💰 낮음 | ⚠️ 별도 관리 | ✅ 높음 | 리전별 서비스, Dev/Prod 분리 |
| **Primary-Replica** | ⭐⭐ 중간 | 💰💰 중간 | ✅ 최종 일관성 | ✅✅ 매우 높음 | DR, 읽기 분산 |
| **Active-Active** | ⭐⭐⭐ 높음 | 💰💰💰 높음 | ⚠️ 충돌 가능 | ✅✅✅ 최고 | 글로벌 쓰기 필요 |
| **글로벌 DB** | ⭐⭐ 중간 | 💰💰💰 높음 | ✅✅ 강한 일관성 | ✅✅✅ 최고 | 글로벌 서비스 |

---

## ⚠️ 중요 고려사항

### 1. 데이터 주권 및 컴플라이언스
```
- GDPR: EU 데이터는 EU 내에서만
- 한국 개인정보보호법: 한국 고객 데이터는 한국 내
- 복제 시 데이터 이동 경로 확인 필요
```

### 2. 네트워크 비용
```
AWS → GCP 데이터 전송
- 복제 시 GB당 비용 발생
- 월 100GB 복제 = 약 $9-15
- 월 1TB 복제 = 약 $90-150
```

### 3. 복제 지연
```
- 크로스 클라우드 복제: 1-5초 지연
- 대륙 간: 5-30초 지연  
- 애플리케이션에서 고려 필요
```

---

## 🎯 최종 권장사항

### CRM 프로젝트: **현재 구성(독립 환경) 유지** ✅

**이유:**
1. ✅ 간단하고 관리하기 쉬움
2. ✅ 각 리전/클라우드 독립 운영 가능
3. ✅ 비용 효율적
4. ✅ 데이터 주권 준수 용이
5. ✅ 향후 필요시 마이그레이션 가능

**언제 복제를 고려해야 하나?**
- ✋ DR이 비즈니스 필수 요구사항일 때
- ✋ 글로벌 읽기 성능이 중요할 때
- ✋ 크로스 리전 데이터 분석이 필요할 때
- ✋ 클라우드 벤더 Lock-in 회피가 필수일 때

---

## 📚 참고 자료

- [AWS DMS Documentation](https://docs.aws.amazon.com/dms/)
- [PostgreSQL Logical Replication](https://www.postgresql.org/docs/current/logical-replication.html)
- [Cloud SQL Replication](https://cloud.google.com/sql/docs/postgres/replication)
- [CockroachDB Multi-Cloud](https://www.cockroachlabs.com/docs/stable/multiregion-overview.html)

---

**작성:** GitHub Copilot CLI  
**버전:** 1.0  
**업데이트:** 2025년 1월
