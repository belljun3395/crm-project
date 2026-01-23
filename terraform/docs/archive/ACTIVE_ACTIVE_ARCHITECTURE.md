# Active-Active 멀티 클라우드 DR 아키텍처

## 🎯 Active-Active 구성

두 클라우드가 **동시에 트래픽을 처리**하며, 한쪽 장애 시 자동으로 다른 쪽이 100% 부하를 받습니다.

```
                    글로벌 로드밸런서
                    (CloudFlare)
                    트래픽 분산 50:50
                           │
          ┌────────────────┴────────────────┐
          ▼                                 ▼
    AWS (Active)                      GCP (Active)
    ┌─────────────┐                  ┌─────────────┐
    │ Application │                  │ Application │
    │ 50% 트래픽  │                  │ 50% 트래픽  │
    ├─────────────┤                  ├─────────────┤
    │ RDS         │◀────양방향 복제──▶│ Cloud SQL   │
    │ Read/Write  │                  │ Read/Write  │
    ├─────────────┤                  ├─────────────┤
    │ ElastiCache │◀────동기화──────▶│ Memorystore │
    │ Read/Write  │                  │ Read/Write  │
    └─────────────┘                  └─────────────┘

장애 시:
  AWS 다운 → GCP가 100% 처리 (30초 이내)
  GCP 다운 → AWS가 100% 처리 (30초 이내)
```

## ⚡ Active-Active의 장점

✅ **즉각적인 장애조치** - 30초~2분 내 전환  
✅ **부하 분산** - 두 클라우드가 트래픽 분담  
✅ **지역 최적화** - 가까운 클라우드로 라우팅  
✅ **높은 가용성** - 99.99% 이상  
✅ **제로 다운타임** - 사용자가 장애를 느끼지 못함

## ⚠️ Active-Active의 과제

1. **데이터베이스 충돌**
   - 해결: 애플리케이션 레벨에서 Primary DB 지정
   - 또는: 글로벌 분산 DB 사용 (CockroachDB, YugabyteDB)

2. **캐시 일관성**
   - 해결: Redis Pub/Sub로 캐시 무효화 동기화
   - 또는: 애플리케이션 레벨 캐시 동기화

3. **세션 공유**
   - 해결: JWT 토큰 사용 (Stateless)
   - 또는: Redis 기반 세션 저장소

---

## 🏗️ 구현 전략

### 전략 A: Single Primary DB (권장, 구현 간단)

```
글로벌 LB (50:50)
     │
     ├─▶ AWS App ──┐
     │             ├─▶ AWS RDS (Primary, Read/Write)
     └─▶ GCP App ──┘       │
                           ▼ 복제
                      GCP Cloud SQL (Replica, Read-Only)

특징:
- 모든 쓰기는 AWS RDS로
- 읽기는 로컬 DB에서 (지연시간 최소)
- 구현 간단
- AWS 장애 시 수동으로 GCP를 Primary로 승격
```

### 전략 B: 글로벌 분산 DB (권장, 완전 자동화)

```
글로벌 LB (50:50)
     │
     ├─▶ AWS App ──┐
     │             ├─▶ CockroachDB Cloud (글로벌)
     └─▶ GCP App ──┘    - Multi-Region
                        - 자동 복제
                        - 양방향 Read/Write

특징:
- 완전 자동 장애조치
- 양쪽 모두 Read/Write 가능
- 지연시간 약간 증가 (50-100ms)
- 추가 비용 발생
```

---

## 📦 구현 모듈

### 1. AWS-GCP VPN 연결

**`terraform/modules/aws-gcp-vpn/`**

### 2. 글로벌 분산 데이터베이스

**옵션 A: CockroachDB Cloud (권장)**
**옵션 B: YugabyteDB Cloud**
**옵션 C: Google Cloud Spanner**

### 3. CloudFlare Load Balancer

**`terraform/modules/cloudflare-lb/`**

### 4. Database Failover Manager

**`terraform/modules/database-failover/`**

### 5. Cache Synchronization

**애플리케이션 레벨 구현**

---

## 🚀 구현 시작

다음 페이지에서 각 모듈의 상세 구현을 진행합니다.

