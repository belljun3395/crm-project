# CRM Infrastructure Architecture

## 개요

CRM 인프라는 AWS 안에서 Kubernetes 기반 애플리케이션 런타임을 운영하는 구조입니다.

- VPC
- EKS
- ECR
- RDS PostgreSQL
- ElastiCache Redis
- Secrets Manager
- SNS / SQS / Scheduler / SES / IAM

## 전체 구성

```text
Internet
  |
  v
AWS VPC
  |
  +-- Public Subnets
  |     +-- NAT Gateway
  |     +-- ALB (DR 환경)
  |
  +-- Private Subnets
        +-- EKS Nodes
        +-- RDS PostgreSQL
        +-- ElastiCache Redis
        +-- MSK (선택)
```

## 네트워크

- VPC 안에 public subnet과 private subnet을 분리합니다.
- EKS worker node는 private subnet에 배치합니다.
- RDS와 ElastiCache도 private subnet에 배치합니다.
- 외부 진입이 필요한 경우 ALB를 public subnet에 둡니다.

## 애플리케이션 런타임

애플리케이션은 Kubernetes 위에서 실행되고, 런타임 구성은 Secrets Manager를 통해 전달됩니다.

주요 런타임 의존성:

- PostgreSQL connection string
- Redis cluster endpoint
- AWS runtime IAM access key
- Scheduler queue and role
- Cache invalidation topic
- SES configuration set

## 데이터 계층

### PostgreSQL

- RDS PostgreSQL 사용
- 환경별 instance class와 backup policy 조정
- EKS node security group에서만 접근 허용

### Redis

- ElastiCache Redis replication group 사용
- auth token, encryption, cluster mode 지원
- EKS node security group에서만 접근 허용

## 메시징과 스케줄링

애플리케이션이 사용하는 AWS 리소스:

- `crm-dr-cache-invalidation-queue-aws`
- `crm_schedule_event_sqs`
- `crm_ses_sqs`
- cache invalidation SNS topic
- EventBridge Scheduler group
- EventBridge Scheduler execution role
- SES configuration set

## 로컬 개발 환경

로컬 개발은 `k8s/local` 패키지로 운영합니다.

- minikube
- MySQL
- Redis cluster
- Kafka
- LocalStack

로컬 환경은 애플리케이션이 기대하는 AWS 계약을 LocalStack으로 맞추고, Kubernetes 배포 방식도 운영 환경과 유사하게 유지합니다.
