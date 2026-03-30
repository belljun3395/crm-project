# AWS Deployment Guide

## 배포 순서

1. 환경 디렉터리로 이동합니다.
2. `terraform.tfvars`를 준비합니다.
3. `terraform init`
4. `terraform plan`
5. `terraform apply`
6. 출력값과 Secrets Manager 값을 애플리케이션 배포에 연결합니다.

예시:

```bash
cd terraform/environments/aws/staging
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan
terraform apply
```

## 환경별 체크리스트

### 공통

- `aws_region`
- `cluster_name`
- `cluster_public_access_cidrs`
- `secrets_manager_secret_name`
- `kafka_bootstrap_servers`

### 데이터 계층

- `rds_master_password`
- `rds_instance_class`
- `rds_allocated_storage`
- `rds_multi_az`
- `elasticache_auth_token`
- `elasticache_node_type`

### 보호 설정

- `rds_deletion_protection`
- `rds_skip_final_snapshot`
- `elasticache_automatic_failover_enabled`
- `elasticache_multi_az_enabled`

## 주요 출력값

- `cluster_endpoint`
- `cluster_certificate_authority_data`
- `ecr_repository_url`
- `app_secret_name`
- `app_secret_arn`
- `rds_endpoint`
- `elasticache_configuration_endpoint`
- `app_runtime_schedule_queue_arn`
- `app_runtime_schedule_role_arn`

## 애플리케이션 연결

애플리케이션은 Secrets Manager의 application secret을 읽도록 연결합니다.
이 시크릿에는 DB, Redis, AWS runtime credential, Scheduler, SNS 설정이 포함됩니다.

## DR 환경

`aws/dr` 환경은 아래 리소스를 추가로 포함합니다.

- ALB
- ALB listener
- target group
- 선택적 MSK

헬스체크 경로는 `/actuator/health`를 사용합니다.

## 적용 전 검증

- `terraform fmt -check`
- `terraform validate`
- `terraform plan`

환경에 `terraform` 바이너리가 없으면 적용 전 별도 실행 환경에서 반드시 검증합니다.
