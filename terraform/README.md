# CRM Terraform Infrastructure

이 디렉터리는 CRM 애플리케이션의 AWS 인프라를 Terraform으로 관리합니다.

## 구성 범위

- VPC, public/private subnet, NAT gateway
- EKS cluster
- ECR repository
- RDS PostgreSQL
- ElastiCache Redis
- Secrets Manager application secret
- 애플리케이션 런타임용 AWS 리소스
  - SNS cache invalidation topic
  - SQS queues
  - EventBridge Scheduler group / role
  - SES configuration set
  - runtime IAM user / access key

## 환경

- `terraform/environments/aws/dev`
- `terraform/environments/aws/staging`
- `terraform/environments/aws/production`
- `terraform/environments/aws/dr`

각 환경은 동일한 모듈 구조를 공유하고, 용량과 보호 설정만 다르게 가져갑니다.

## 런타임 시크릿 계약

각 환경은 Secrets Manager에 애플리케이션 런타임 값을 JSON 형태로 저장합니다.

- `DATABASE_URL`
- `MASTER_DATABASE_URL`
- `REPLICA_DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `REDIS_HOST`
- `REDIS_NODES`
- `REDIS_PASSWORD`
- `REDIS_MAX_REDIRECTS`
- `AWS_ACCESS_KEY`
- `AWS_SECRET_KEY`
- `AWS_CONFIGURATION_SET`
- `AWS_SCHEDULE_ROLE_ARN`
- `AWS_SCHEDULE_SQS_ARN`
- `AWS_SCHEDULE_GROUP_NAME`
- `AWS_SNS_CACHE_INVALIDATION_TOPIC_ARN`
- `CLOUD_PROVIDER`
- `MAIL_PROVIDER`
- `SCHEDULER_PROVIDER`
- `KAFKA_BOOTSTRAP_SERVERS`

기본값 생성이 가능한 항목은 인프라에서 자동 생성하고, 환경별 override가 필요한 값은 변수나 `additional_secret_values`로 주입합니다.

## 빠른 시작

```bash
cd terraform/environments/aws/dev
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan
terraform apply
```

적용 전 확인 항목:

- `cluster_public_access_cidrs`
- `rds_master_password`
- `elasticache_auth_token`
- `kafka_bootstrap_servers`
- `secrets_manager_secret_name`

## 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md)
- [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)
- [MODULES.md](./MODULES.md)

## 운영 메모

- 애플리케이션 코드에는 `crm_schedule_event_sqs`, `crm_ses_sqs`, `crm-dr-cache-invalidation-queue-aws` 같은 큐 이름이 고정돼 있습니다.
- 같은 AWS 계정과 리전에서 여러 환경을 동시에 운영하려면 queue naming 전략도 함께 관리해야 합니다.
- PostgreSQL 전환 작업은 애플리케이션 브랜치와 함께 맞춰야 하며, 이 Terraform 패키지는 그 인프라 수용면을 제공합니다.
