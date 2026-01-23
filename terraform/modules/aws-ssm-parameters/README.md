# AWS SSM Parameters Module

AWS Systems Manager Parameter Store를 관리하는 Terraform 모듈입니다.

## 기능

- SSM 파라미터 생성
- SecureString 지원
- 계층적 파라미터 구조
- KMS 암호화 지원

## 사용 예시

```hcl
module "ssm_parameters" {
  source = "../../modules/aws-ssm-parameters"

  parameters = {
    "/crm/dev/database/host" = {
      value       = "db.example.com"
      type        = "String"
      description = "Database host"
    }
    "/crm/dev/database/password" = {
      value       = "secret-password"
      type        = "SecureString"
      description = "Database password"
    }
  }
  
  kms_key_id = "arn:aws:kms:..."
  
  tags = {
    Environment = "dev"
    Project     = "crm"
  }
}
```

## 입력 변수

| 변수 | 설명 | 타입 | 기본값 | 필수 |
|------|------|------|--------|------|
| parameters | 파라미터 맵 | map(object) | {} | yes |
| kms_key_id | KMS 키 ID (SecureString용) | string | null | no |
| tags | 리소스 태그 | map(string) | {} | no |

## 출력 값

| 출력 | 설명 |
|------|------|
| parameter_arns | 파라미터 ARN 맵 |
| parameter_names | 파라미터 이름 리스트 |

## 요구사항

- Terraform >= 1.5.0
- AWS Provider >= 5.0
