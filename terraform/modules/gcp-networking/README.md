# GCP Networking Module

GCP VPC, 서브넷, Cloud NAT를 생성하는 Terraform 모듈입니다.

## 기능

- VPC 네트워크 생성
- 서브넷 생성 (Primary + Secondary ranges)
- Cloud Router 구성
- Cloud NAT 설정

## 사용 예시

```hcl
module "networking" {
  source = "../../modules/gcp-networking"

  project_id   = "my-project-id"
  network_name = "crm-dev"
  region       = "asia-northeast3"
  
  subnet_ip_cidr_range           = "10.20.0.0/20"
  subnet_secondary_pods_cidr     = "10.21.0.0/16"
  subnet_secondary_services_cidr = "10.22.0.0/20"
  
  ip_range_pods_name     = "crm-dev-pods"
  ip_range_services_name = "crm-dev-services"
  
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
| network_name | VPC 네트워크 이름 | string | - | yes |
| region | 리전 | string | - | yes |
| subnet_ip_cidr_range | 서브넷 CIDR | string | - | yes |
| subnet_secondary_pods_cidr | Pod IP 범위 | string | - | yes |
| subnet_secondary_services_cidr | Service IP 범위 | string | - | yes |
| labels | 리소스 라벨 | map(string) | {} | no |

## 출력 값

| 출력 | 설명 |
|------|------|
| network_id | VPC 네트워크 ID |
| network_name | VPC 네트워크 이름 |
| subnet_id | 서브넷 ID |
| subnet_name | 서브넷 이름 |

## 요구사항

- Terraform >= 1.5.0
- Google Provider >= 5.0
