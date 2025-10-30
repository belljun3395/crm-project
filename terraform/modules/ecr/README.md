# AWS ECR Module

Amazon ECR 컨테이너 레지스트리를 생성하는 Terraform 모듈입니다.

## 기능

- ECR 레포지토리 생성
- 이미지 스캔 설정
- 라이프사이클 정책 구성
- 암호화 설정

## 사용 예시

```hcl
module "ecr" {
  source = "../../modules/ecr"

  repository_name = "crm-app"
  
  image_tag_mutability = "MUTABLE"
  scan_on_push         = true
  
  tags = {
    Environment = "dev"
    Project     = "crm"
  }
}
```

## 입력 변수

| 변수 | 설명 | 타입 | 기본값 | 필수 |
|------|------|------|--------|------|
| repository_name | ECR 레포지토리 이름 | string | - | yes |
| image_tag_mutability | 이미지 태그 변경 가능 여부 | string | "MUTABLE" | no |
| scan_on_push | 푸시 시 이미지 스캔 | bool | true | no |
| encryption_type | 암호화 타입 | string | "AES256" | no |
| tags | 리소스 태그 | map(string) | {} | no |

## 출력 값

| 출력 | 설명 |
|------|------|
| repository_url | ECR 레포지토리 URL |
| repository_arn | ECR 레포지토리 ARN |
| registry_id | ECR 레지스트리 ID |

## 요구사항

- Terraform >= 1.5.0
- AWS Provider >= 5.0
