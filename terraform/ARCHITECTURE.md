# CRM 프로젝트 아키텍처 가이드

멀티 클라우드 Active-Active DR 아키텍처 상세 설명

## 목차

1. [전체 아키텍처](#전체-아키텍처)
2. [네트워크 아키텍처](#네트워크-아키텍처)
3. [DR 전략](#dr-전략)
4. [데이터베이스 복제](#데이터베이스-복제)
5. [보안 아키텍처](#보안-아키텍처)
6. [모니터링](#모니터링)

---

## 전체 아키텍처

### Active-Active DR 구성

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      CloudFlare 글로벌 로드밸런서                             │
│                  (Health Check + Dynamic Latency Routing)                   │
└────────────┬──────────────────────────────────────────────┬─────────────────┘
             │                                              │
             ▼ 50% (자동 분산)                              ▼ 50% (자동 분산)
    ┌────────────────────────┐                    ┌────────────────────────┐
    │   AWS (Active)         │                    │   GCP (Active)         │
    │   Region: ap-northeast-2                    │   Region: asia-northeast3
    │                        │                    │                        │
    │  ┌──────────────────┐ │                    │  ┌──────────────────┐ │
    │  │ Application LB   │ │                    │  │ Global LB        │ │
    │  │ (ALB)            │ │                    │  │                  │ │
    │  └────────┬─────────┘ │                    │  └────────┬─────────┘ │
    │           │            │                    │           │            │
    │           ▼            │                    │           ▼            │
    │  ┌──────────────────┐ │                    │  ┌──────────────────┐ │
    │  │ EKS Cluster      │ │                    │  │ GKE Cluster      │ │
    │  │ v1.29            │ │                    │  │ REGULAR channel  │ │
    │  │                  │ │                    │  │                  │ │
    │  │ - 3-10 nodes     │ │                    │  │ - 3-10 nodes     │ │
    │  │ - t3.xlarge      │ │                    │  │ - n2-standard-16 │ │
    │  └────────┬─────────┘ │                    │  └────────┬─────────┘ │
    │           │            │                    │           │            │
    │           ├────────────┼────────────────────┼───────────┤            │
    │           │            │     VPN Tunnel     │           │            │
    │           │            │   (HA, BGP 라우팅)  │           │            │
    │           ├────────────┼────────────────────┼───────────┤            │
    │           │            │                    │           │            │
    │           ▼            │                    │           ▼            │
    │  ┌──────────────────┐ │                    │  ┌──────────────────┐ │
    │  │ RDS PostgreSQL   │ │◀─ Logical Rep. ────│  │ Cloud SQL        │ │
    │  │ (Primary)        │ │   (1-5초 지연)     │  │ (Replica)        │ │
    │  │                  │ │                    │  │                  │ │
    │  │ - Multi-AZ       │ │                    │  │ - Regional HA    │ │
    │  │ - Auto Backup    │ │                    │  │ - PITR          │ │
    │  └──────────────────┘ │                    │  └──────────────────┘ │
    │                        │                    │                        │
    │  ┌──────────────────┐ │                    │  ┌──────────────────┐ │
    │  │ ElastiCache      │ │◀─ App Level ───────│  │ Memorystore      │ │
    │  │ Redis (Multi-AZ) │ │   Sync             │  │ Redis (HA)       │ │
    │  └──────────────────┘ │                    │  └──────────────────┘ │
    │                        │                    │                        │
    └────────────────────────┘                    └────────────────────────┘
```

### 주요 특징

#### 1. 글로벌 트래픽 관리
- CloudFlare Load Balancer가 전 세계 사용자 요청을 지능적으로 분산
- Dynamic Latency Routing: 사용자에게 가장 빠른 클라우드로 자동 라우팅
- Health Check: 60초마다 각 클라우드 상태 확인
- 자동 장애조치: 30초 내 unhealthy 클라우드 제외

#### 2. 양방향 Active 구성
- 정상 시 AWS와 GCP 모두 활성 상태로 트래픽 처리
- 각 클라우드는 독립적으로 읽기/쓰기 가능
- 부하 분산으로 인한 성능 향상

#### 3. 데이터 일관성
- **데이터베이스**: PostgreSQL Logical Replication (단방향)
  - AWS RDS → GCP Cloud SQL
  - 복제 지연: 1-5초
  - GCP 장애 시 AWS만 사용 (변경 없음)
  - AWS 장애 시 GCP를 Primary로 수동 승격

- **캐시**: 애플리케이션 레벨 동기화
  - 각 클라우드가 독립적인 캐시 유지
  - 캐시 무효화 이벤트는 메시지 큐로 동기화

---

## 네트워크 아키텍처

### VPN 연결

```
AWS VPC (10.30.0.0/16)              GCP VPC (10.20.0.0/20)
┌──────────────────────┐            ┌──────────────────────┐
│                      │            │                      │
│  VPN Gateway         │            │  HA VPN Gateway      │
│  ┌────────┐          │            │  ┌────────┐          │
│  │Interface│          │  Tunnel 1 │  │Interface│          │
│  │   0    │◀─────────┼────────────┼─▶│   0    │          │
│  └────────┘          │ 169.254.1.0/30 └────────┘          │
│                      │            │                      │
│  ┌────────┐          │            │  ┌────────┐          │
│  │Interface│          │  Tunnel 2 │  │Interface│          │
│  │   1    │◀─────────┼────────────┼─▶│   1    │          │
│  └────────┘          │ 169.254.2.0/30 └────────┘          │
│                      │            │                      │
│  BGP ASN: 64512      │            │  BGP ASN: 65000      │
└──────────────────────┘            └──────────────────────┘

특징:
- 이중화된 VPN 터널 (고가용성)
- BGP 동적 라우팅
- IPsec 암호화
- 데이터베이스 복제 및 관리 트래픽 전용
```

### 서브넷 구성

#### AWS
```
VPC: 10.30.0.0/16
├── Public Subnets
│   ├── 10.30.0.0/24  (ap-northeast-2a) - ALB, NAT
│   └── 10.30.1.0/24  (ap-northeast-2c) - ALB, NAT
│
└── Private Subnets
    ├── 10.30.10.0/24 (ap-northeast-2a) - EKS, RDS, ElastiCache
    └── 10.30.11.0/24 (ap-northeast-2c) - EKS, RDS, ElastiCache
```

#### GCP
```
VPC: 10.20.0.0/20
├── Primary Range: 10.20.0.0/20 - GKE nodes
├── Secondary Range (Pods): 10.21.0.0/16
└── Secondary Range (Services): 10.22.0.0/20
```

---

## DR 전략

### Active-Active 장애 시나리오

#### 시나리오 1: AWS 전체 장애

```
1. 장애 발생
   AWS Region 전체 다운

2. 감지 (30초)
   CloudFlare Health Check 실패
   3번 연속 실패 후 unhealthy 판정

3. 자동 조치 (30초)
   CloudFlare가 AWS pool을 제외
   100% 트래픽을 GCP로 전환

4. 수동 조치 필요 (2-3분)
   GCP Cloud SQL을 Primary로 승격
   애플리케이션 DB 연결 문자열 변경
   
5. 서비스 복구
   GCP에서 100% 트래픽 처리
   RTO: 2-5분
   RPO: 5초 (복제 지연)

6. AWS 복구 후
   데이터 동기화 (GCP → AWS)
   Health Check 통과 후 자동 포함
   트래픽 점진적 복구 (50:50)
```

#### 시나리오 2: GCP 전체 장애

```
1. 장애 발생
   GCP Region 전체 다운

2. 감지 (30초)
   CloudFlare Health Check 실패

3. 자동 조치 (30초)
   CloudFlare가 GCP pool을 제외
   100% 트래픽을 AWS로 전환

4. 추가 조치 불필요
   AWS RDS는 이미 Primary
   즉시 서비스 가능
   
5. 서비스 복구
   AWS에서 100% 트래픽 처리
   RTO: 30초-2분
   RPO: 0 (Primary DB 사용)

6. GCP 복구 후
   Cloud SQL 복제 재개
   Health Check 통과 후 자동 포함
   트래픽 점진적 복구 (50:50)
```

### RTO/RPO 목표

| 시나리오 | RTO | RPO | 설명 |
|----------|-----|-----|------|
| AWS 장애 | 2-5분 | 5초 | Cloud SQL Primary 승격 필요 |
| GCP 장애 | 30초-2분 | 0초 | 추가 조치 불필요 |
| 부분 장애 | 30초 | 0초 | 자동 장애조치 |

---

## 데이터베이스 복제

### PostgreSQL Logical Replication

#### 복제 흐름

```
AWS RDS (Primary)
    │
    │ 1. WAL (Write-Ahead Log) 생성
    │
    │ 2. Publication 생성
    │    CREATE PUBLICATION crm_replication FOR ALL TABLES;
    │
    │ 3. Replication Slot
    │    복제 진행 상황 추적
    │
    ▼
 VPN Tunnel (프라이빗 연결)
    │
    ▼
GCP Cloud SQL (Replica)
    │
    │ 4. Subscription 생성
    │    CREATE SUBSCRIPTION crm_subscription ...
    │
    │ 5. 데이터 적용
    │    실시간으로 변경사항 반영
    │
    └─ Read-Only 모드
```

#### 복제 설정

**AWS RDS 설정:**
```sql
-- Parameter Group 설정
rds.logical_replication = 1
max_wal_senders = 10
max_replication_slots = 10

-- Publication 생성
CREATE PUBLICATION crm_replication FOR ALL TABLES;

-- 복제 사용자 권한
GRANT SELECT ON ALL TABLES IN SCHEMA public TO replication_user;
```

**GCP Cloud SQL 설정:**
```sql
-- Subscription 생성 (VPN 통한 프라이빗 IP)
CREATE SUBSCRIPTION crm_subscription
CONNECTION 'host=10.30.10.5 port=5432 dbname=crm user=repl_user password=xxx'
PUBLICATION crm_replication;

-- 복제 상태 확인
SELECT * FROM pg_stat_subscription;
```

#### 복제 모니터링

```sql
-- AWS RDS: 복제 지연 확인
SELECT 
    client_addr,
    state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn,
    sync_state
FROM pg_stat_replication;

-- GCP Cloud SQL: 복제 상태
SELECT 
    subname,
    status,
    received_lsn,
    latest_end_lsn,
    latest_end_time
FROM pg_stat_subscription;
```

### Cloud SQL을 Primary로 승격

AWS 장애 시 GCP를 Primary로 만들기:

```sql
-- 1. Subscription 삭제 (Read-Only 해제)
DROP SUBSCRIPTION crm_subscription;

-- 2. 이제 Cloud SQL이 Primary
-- 애플리케이션에서 쓰기 가능

-- 3. 새로운 Publication 생성 (AWS 복구 후)
CREATE PUBLICATION crm_replication FOR ALL TABLES;
```

---

## 보안 아키텍처

### 네트워크 보안

#### AWS
```
Security Groups:
├── EKS Cluster SG
│   ├── Ingress: ALB에서만 (80, 443)
│   └── Egress: All (DB, Cache 접근)
│
├── RDS SG
│   ├── Ingress: EKS SG + GCP CIDR (5432)
│   └── Egress: None
│
├── ElastiCache SG
│   ├── Ingress: EKS SG + GCP CIDR (6379)
│   └── Egress: None
│
└── VPN SG
    ├── Ingress: GCP Gateway IP (UDP 500, 4500)
    └── Egress: All
```

#### GCP
```
Firewall Rules:
├── GKE Nodes
│   ├── Allow: Load Balancer (80, 443)
│   └── Allow: VPN CIDR (모든 포트)
│
├── Cloud SQL
│   ├── Allow: GKE Nodes (5432)
│   └── Allow: AWS CIDR via VPN (5432)
│
├── Memorystore
│   ├── Allow: GKE Nodes (6379)
│   └── Allow: AWS CIDR via VPN (6379)
│
└── VPN Gateway
    └── Allow: AWS Gateway IP (UDP 500, 4500)
```

### 암호화

#### 전송 중 암호화
- VPN: IPsec 암호화
- 데이터베이스: SSL/TLS 필수
- 캐시: TLS 활성화
- API: HTTPS 전용

#### 저장 시 암호화
- RDS: KMS 암호화
- Cloud SQL: CMEK 지원
- ElastiCache: At-rest 암호화
- Memorystore: 자동 암호화

### 시크릿 관리

```
AWS Secrets Manager                GCP Secret Manager
├── Database Credentials           ├── Database Credentials
├── Redis Auth Token              ├── Redis Auth Token
├── API Keys                      ├── API Keys
└── Application Secrets           └── Application Secrets

애플리케이션은 각 클라우드의 Secret Manager에서
자동으로 시크릿을 가져옴 (IAM 기반 인증)
```

---

## 모니터링

### CloudFlare 모니터링

- Pool Health: AWS/GCP 각 풀의 상태
- Traffic Distribution: 트래픽 분산 비율
- Response Time: Origin별 응답 시간
- Failover Events: 장애조치 이벤트 로그

### AWS 모니터링

**CloudWatch Metrics:**
- EKS Cluster 메트릭
- RDS 성능 메트릭 (CPU, 메모리, 연결)
- ElastiCache 메트릭
- VPN 터널 상태

### GCP 모니터링

**Cloud Monitoring:**
- GKE Cluster 메트릭
- Cloud SQL 메트릭
- Memorystore 메트릭
- VPN 터널 상태

### 복제 모니터링

**중요 메트릭:**
```sql
-- 복제 지연 시간
SELECT 
    EXTRACT(EPOCH FROM (NOW() - pg_last_xact_replay_timestamp())) 
    AS replication_lag_seconds;

-- 복제 슬롯 상태
SELECT slot_name, active, restart_lsn 
FROM pg_replication_slots;
```

**알람 설정:**
- 복제 지연 > 10초
- Replication slot inactive
- VPN 터널 다운
- Health check 실패

---

## 성능 최적화

### 데이터베이스

**Connection Pooling:**
```yaml
# PgBouncer 설정
apiVersion: v1
kind: ConfigMap
metadata:
  name: pgbouncer-config
data:
  pgbouncer.ini: |
    [databases]
    crm = host=rds-endpoint port=5432 dbname=crm
    
    [pgbouncer]
    pool_mode = transaction
    max_client_conn = 1000
    default_pool_size = 25
```

### 캐시

**Redis 최적화:**
```
# ElastiCache/Memorystore 설정
maxmemory-policy: allkeys-lru
timeout: 300
tcp-keepalive: 60
```

### 네트워크

**VPN 최적화:**
- MTU 크기 조정: 1400 (터널 오버헤드 고려)
- TCP Window Scaling 활성화
- BGP Keepalive: 60초

---

## 참고 자료

- [PostgreSQL Logical Replication](https://www.postgresql.org/docs/current/logical-replication.html)
- [AWS VPN Documentation](https://docs.aws.amazon.com/vpn/)
- [GCP VPN Documentation](https://cloud.google.com/network-connectivity/docs/vpn)
- [CloudFlare Load Balancing](https://developers.cloudflare.com/load-balancing/)

---

**최종 업데이트**: 2025년 1월  
**버전**: 2.0
