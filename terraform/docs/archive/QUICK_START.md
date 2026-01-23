# CRM 프로젝트 Terraform 빠른 시작 가이드

## 🎯 개요

이 가이드는 CRM 프로젝트의 멀티 클라우드 인프라를 Terraform으로 배포하는 방법을 설명합니다.

## 📁 프로젝트 구조

```
terraform/
├── modules/           # 재사용 가능한 인프라 모듈
└── environments/      # 환경별 구성 (dev/staging/production)
    ├── aws/          # AWS 인프라
    └── gcp/          # GCP 인프라
```

## 🚀 빠른 시작

### 1. 사전 요구사항

```bash
# Terraform 설치 확인
terraform version  # >= 1.5.0 필요

# AWS CLI 설정
aws configure

# GCP CLI 설정
gcloud auth application-default login
gcloud config set project YOUR_PROJECT_ID
```

### 2. 개발 환경 배포 (AWS)

```bash
# 1. 환경으로 이동
cd terraform/environments/aws/dev

# 2. 변수 파일 생성
cp terraform.tfvars.example terraform.tfvars

# 3. 필수 변수 설정
cat > terraform.tfvars << EOL
aws_region = "ap-northeast-2"
cluster_name = "crm-dev"
project = "crm"
environment = "dev"

# RDS 활성화 (선택사항)
enable_rds = true
rds_master_password = "CHANGE_ME_SECURE_PASSWORD"

# ElastiCache 활성화 (선택사항)
enable_elasticache = true
elasticache_auth_token = "CHANGE_ME_SECURE_TOKEN_16_CHARS_MIN"

# 애플리케이션 시크릿
app_env = {
  DATABASE_URL = "postgresql://user:pass@host:5432/db"
  DATABASE_USERNAME = "crmadmin"
  DATABASE_PASSWORD = "secure_password"
  REDIS_HOST = "redis-host"
  REDIS_PASSWORD = "redis_password"
  # ... 기타 필요한 환경 변수
}
EOL

# 4. Terraform 초기화
terraform init

# 5. 실행 계획 확인
terraform plan

# 6. 배포
terraform apply
```

### 3. 개발 환경 배포 (GCP)

```bash
# 1. 환경으로 이동
cd terraform/environments/gcp/dev

# 2. 변수 파일 생성
cp terraform.tfvars.example terraform.tfvars

# 3. 필수 변수 설정
cat > terraform.tfvars << EOL
project_id = "your-gcp-project-id"
region = "asia-northeast3"
cluster_name = "crm-dev-gke"

# Cloud SQL 활성화 (선택사항)
enable_cloud_sql = true
cloud_sql_user_password = "CHANGE_ME_SECURE_PASSWORD"

# Memorystore 활성화 (선택사항)
enable_memorystore = true

# Secret Manager 활성화 (선택사항)
enable_secret_manager = true
secret_manager_secret_data = jsonencode({
  DATABASE_URL = "postgresql://user:pass@host:5432/db"
  REDIS_HOST = "redis-host"
  # ... 기타 필요한 환경 변수
})
EOL

# 4. Terraform 초기화
terraform init

# 5. 실행 계획 확인
terraform plan

# 6. 배포
terraform apply
```

## 🔧 환경별 배포

### Development (개발)
- **목적**: 개발 및 테스트
- **특징**: 최소 리소스, 비용 최적화
- **데이터베이스/캐시**: 선택적 활성화

```bash
cd terraform/environments/{aws|gcp}/dev
terraform apply
```

### Staging (스테이징)
- **목적**: 프로덕션 유사 환경 테스트
- **특징**: 중간 규모 리소스, Multi-AZ/Regional HA
- **데이터베이스/캐시**: 기본 활성화

```bash
cd terraform/environments/{aws|gcp}/staging
terraform apply
```

### Production (프로덕션)
- **목적**: 실제 서비스 운영
- **특징**: 대규모 리소스, 완전한 HA, 삭제 방지
- **데이터베이스/캐시**: 완전 활성화

```bash
cd terraform/environments/{aws|gcp}/production
terraform apply
```

## 📊 출력값 확인

배포 후 중요한 정보 확인:

```bash
# AWS
terraform output cluster_endpoint          # EKS API endpoint
terraform output ecr_repository_url        # ECR 레포지토리
terraform output rds_endpoint              # 데이터베이스 엔드포인트
terraform output elasticache_primary_endpoint  # Redis 엔드포인트

# GCP
terraform output gke_cluster_endpoint      # GKE API endpoint
terraform output artifact_registry_repository_id  # Artifact Registry
terraform output cloud_sql_connection_name # 데이터베이스 연결명
terraform output memorystore_host          # Redis 호스트
```

## 🔐 보안 주의사항

### ⚠️ 절대 하지 말아야 할 것

```bash
# ❌ terraform.tfvars를 Git에 커밋하지 마세요!
git add terraform.tfvars  # 절대 금지!

# ✅ .gitignore에 추가되어 있는지 확인
cat .gitignore | grep terraform.tfvars
```

### ✅ 권장 사항

1. **민감한 정보 관리**
   - AWS Secrets Manager 사용
   - GCP Secret Manager 사용
   - 환경 변수 활용

2. **강력한 비밀번호 생성**
   ```bash
   # 안전한 비밀번호 생성
   openssl rand -base64 32
   ```

3. **접근 제어**
   - IAM 권한 최소화
   - MFA 활성화
   - IP 화이트리스트 설정

## 🗑️ 리소스 정리

```bash
# 개발 환경 삭제
cd terraform/environments/{aws|gcp}/dev
terraform destroy

# ⚠️ Production 환경은 삭제 방지가 활성화되어 있습니다!
# 먼저 deletion_protection을 false로 변경한 후 destroy 실행
```

## 🔄 업데이트 및 변경

```bash
# 1. 코드 변경 후 계획 확인
terraform plan

# 2. 변경사항 적용
terraform apply

# 3. 특정 리소스만 업데이트
terraform apply -target=module.eks

# 4. 상태 새로고침
terraform refresh
```

## 📚 추가 문서

- [INFRASTRUCTURE_REVIEW.md](./INFRASTRUCTURE_REVIEW.md) - 전체 인프라 리뷰
- [INFRASTRUCTURE_IMPROVEMENTS_COMPLETED.md](./INFRASTRUCTURE_IMPROVEMENTS_COMPLETED.md) - 개선 완료 보고서
- [modules/*/README.md](./modules/) - 각 모듈별 상세 가이드

## 🆘 문제 해결

### 일반적인 오류

1. **"Error: Insufficient permissions"**
   ```bash
   # AWS/GCP 자격증명 확인
   aws sts get-caller-identity
   gcloud auth list
   ```

2. **"Error: Backend initialization required"**
   ```bash
   terraform init -reconfigure
   ```

3. **"Error: Resource already exists"**
   ```bash
   # 상태 가져오기
   terraform import <resource_type>.<resource_name> <resource_id>
   ```

### 디버깅

```bash
# 상세 로그 출력
export TF_LOG=DEBUG
terraform apply

# 로그 파일 저장
export TF_LOG_PATH=./terraform.log
terraform apply
```

## 💡 팁

1. **비용 확인**: 배포 전 `terraform plan`으로 예상 비용 확인
2. **점진적 배포**: dev → staging → production 순서로 배포
3. **백업**: 프로덕션 배포 전 현재 상태 백업
4. **모니터링**: 배포 후 CloudWatch/Cloud Monitoring으로 리소스 상태 확인
5. **문서화**: 변경사항을 항상 문서화

## 📞 지원

문제가 발생하면:
1. 관련 모듈의 README.md 확인
2. Terraform 공식 문서 참조
3. 팀 내부 문서 확인

---

**작성:** GitHub Copilot CLI  
**버전:** 1.0  
**업데이트:** 2025년 1월
