# CRM 프로젝트 - 멀티 클라우드 인프라

Active-Active 재해복구(DR)를 지원하는 멀티 클라우드 Kubernetes 인프라

[![Terraform](https://img.shields.io/badge/Terraform-1.5+-purple?logo=terraform)](https://www.terraform.io/)
[![AWS](https://img.shields.io/badge/AWS-EKS-orange?logo=amazon-aws)](https://aws.amazon.com/)
[![GCP](https://img.shields.io/badge/GCP-GKE-blue?logo=google-cloud)](https://cloud.google.com/)

## 목차

- [개요](#개요)
- [아키텍처](#아키텍처)
- [주요 기능](#주요-기능)
- [빠른 시작](#빠른-시작)
- [문서](#문서)
- [디렉터리 구조](#디렉터리-구조)

---

## 개요

이 프로젝트는 AWS와 GCP에서 **Active-Active 재해복구(DR)** 구성을 제공하는 프로덕션급 멀티 클라우드 Kubernetes 인프라입니다.

### 핵심 특징

- Active-Active DR: 양쪽 클라우드가 동시에 트래픽 처리
- 자동 장애조치: 30초 내 자동 전환
- 데이터베이스 복제: PostgreSQL 실시간 복제 (1-5초 지연)
- 글로벌 로드밸런싱: CloudFlare 기반 지능형 라우팅
- 고가용성: 99.99% 가용성 목표 (Multi-AZ NAT, EKS/GKE HA 구성)
- 보안 강화: Kubernetes 제어부 프라이빗 액세스 제한, VPN 기반 내부 통신
- 모듈화: 재사용 가능한 Terraform 모듈

---

## 아키텍처

### Active-Active DR 구성

```
                    CloudFlare 글로벌 로드밸런서
                    (Dynamic Latency Routing)
                             │
              ┌──────────────┴──────────────┐
              ▼ 50%                      50% ▼
        AWS (Active)                GCP (Active)
        ┌──────────────┐            ┌──────────────┐
        │ EKS Cluster  │            │ GKE Cluster  │
        │              │            │              │
        │ RDS          │◀─ 복제 ────│ Cloud SQL    │
        │ (Primary)    │   VPN      │ (Replica)    │
        │              │            │              │
        │ ElastiCache  │◀─ 동기화 ──│ Memorystore  │
        │              │            │              │
        │ ALB          │            │ Load Balancer│
        └──────────────┘            └──────────────┘
```

상세 아키텍처는 [ARCHITECTURE.md](./ARCHITECTURE.md) 참조

---

## 주요 기능

### 인프라 컴포넌트

#### AWS 환경
- EKS: Kubernetes 클러스터
- RDS: PostgreSQL (Primary)
- ElastiCache: Redis (Multi-AZ)
- MSK: Kafka (Event Streaming)
- VPN: AWS-GCP 연결

#### GCP 환경
- GKE: Kubernetes 클러스터
- Cloud SQL: PostgreSQL (Replica)
- Memorystore: Redis (HA)
- VPN: GCP-AWS 연결

### 환경 구성

- dev: 개발 환경 (최소 리소스)
- staging: 스테이징 (중간 리소스, HA)
- production: 프로덕션 (대규모 리소스, 완전 HA)
- dr: DR 환경 (Active-Active, 양쪽 클라우드)

---

## 빠른 시작

### 개발 환경 배포

```bash
# AWS 개발 환경
cd terraform/environments/aws/dev
terraform init
terraform apply

# GCP 개발 환경
cd terraform/environments/gcp/dev
terraform init
terraform apply
```

### DR 환경 배포

상세 가이드는 [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) 참조

---

## 문서

| 문서 | 설명 |
|------|------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 전체 아키텍처 및 DR 전략 |
| [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) | 배포 가이드 (Active-Active 포함) |
| [MODULES.md](./MODULES.md) | 모듈 레퍼런스 |

---

## 디렉터리 구조

```
terraform/
├── modules/               # 15개 모듈
│   ├── AWS (7개)
│   ├── GCP (6개)
│   └── DR (2개)
│
├── environments/          # 8개 환경
│   ├── aws/
│   │   ├── dev/
│   │   ├── staging/
│   │   ├── production/
│   │   └── dr/
│   └── gcp/
│       ├── dev/
│       ├── staging/
│       ├── production/
│       └── dr/
│
└── docs/                  # 추가 문서
```

---

## 예상 비용 (월간)

| 환경 | AWS | GCP | 합계 |
|------|-----|-----|------|
| dev | $200 | $150 | $350 |
| staging | $600 | $500 | $1,100 |
| production | $1,200 | $1,000 | $2,200 |
| **dr** | **$1,059** | **$959** | **$2,268** |

*DR 환경은 CloudFlare ($50) 및 데이터 전송 비용 포함

---

## 성능 지표 (DR)

- 가용성: 99.99% (연간 다운타임 < 53분)
- RTO: 30초-5분
- RPO: 5초
- 응답 시간: p95 < 200ms

---

**최종 업데이트**: 2025년 1월  
**버전**: 2.0
