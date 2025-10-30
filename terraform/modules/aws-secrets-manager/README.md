# AWS Secrets Manager Module

AWS Secrets Manager로 시크릿을 관리하는 Terraform 모듈입니다.

## 기능

- 시크릿 생성 및 버전 관리
- KMS 암호화 지원
- 자동 회전 설정
- 복구 기간 설정

## 사용 예시

```hcl
module "app_secret" {
  source = "../../modules/aws-secrets-manager"

  secret_name = "crm/dev/application"
  description = "CRM application secrets"
  
  secret_string_values = {
    DATABASE_URL = "postgresql://..."
    API_KEY      = "secret-key"
  }
  
  kms_key_id              = "arn:aws:kms:..."
  recovery_window_in_days = 7
  
  tags = {
    Environment = "dev"
    Project     = "crm"
  }
}
```

## 입력 변수

| 변수 | 설명 | 타입 | 기본값 | 필수 |
|------|------|------|--------|------|
| secret_name | 시크릿 이름 | string | - | yes |
| description | 시크릿 설명 | string | "" | no |
| secret_string_values | 시크릿 값 (key-value) | map(string) | {} | no |
| kms_key_id | KMS 키 ID | string | null | no |
| recovery_window_in_days | 복구 대기 기간 (일) | number | 30 | no |
| tags | 리소스 태그 | map(string) | {} | no |

## 출력 값

| 출력 | 설명 |
|------|------|
| secret_id | 시크릿 ID |
| secret_arn | 시크릿 ARN |
| secret_name | 시크릿 이름 |

## 요구사항

- Terraform >= 1.5.0
- AWS Provider >= 5.0
