# GCP Artifact Registry Module

Google Artifact Registry를 생성하는 Terraform 모듈입니다.

## 기능

- Artifact Registry 레포지토리 생성
- Docker 이미지 저장
- 취약점 스캔 지원
- IAM 정책 관리

## 사용 예시

```hcl
module "artifact_registry" {
  source = "../../modules/artifact_registry"

  project_id    = "my-project-id"
  location      = "asia-northeast3"
  repository_id = "crm-app"
  format        = "DOCKER"
  description   = "CRM application container registry"
  
  labels = {
    environment = "dev"
    project     = "crm"
  }
}
```

## 입력 변수

| 변수 | 설명 | 타입 | 기본값 | 필수 |
|------|------|------|--------|------|
| project_id | GCP 프로젝트 ID | string | - | yes |
| location | 리전/멀티 리전 | string | - | yes |
| repository_id | 레포지토리 ID | string | - | yes |
| format | 레포지토리 포맷 | string | "DOCKER" | no |
| description | 레포지토리 설명 | string | "" | no |
| labels | 리소스 라벨 | map(string) | {} | no |

## 출력 값

| 출력 | 설명 |
|------|------|
| repository_id | 레포지토리 ID |
| repository_name | 레포지토리 전체 이름 |
| location | 레포지토리 위치 |

## 요구사항

- Terraform >= 1.5.0
- Google Provider >= 5.0
