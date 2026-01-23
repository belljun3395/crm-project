# AWS Networking Module

VPC, 서브넷, NAT Gateway를 생성하는 Terraform 모듈입니다.

## 기능

- VPC 생성
- Public/Private 서브넷 생성
- NAT Gateway 구성
- Internet Gateway 생성
- Route Table 설정

## 사용 예시

```hcl
module "networking" {
  source = "../../modules/networking"

  name                 = "crm-dev"
  vpc_cidr             = "10.10.0.0/16"
  azs                  = ["ap-northeast-2a", "ap-northeast-2c"]
  public_subnet_cidrs  = ["10.10.0.0/24", "10.10.1.0/24"]
  private_subnet_cidrs = ["10.10.10.0/24", "10.10.11.0/24"]
  enable_nat           = true
  
  tags = {
    Environment = "dev"
    Project     = "crm"
  }
}
```

## 입력 변수

| 변수 | 설명 | 타입 | 기본값 | 필수 |
|------|------|------|--------|------|
| name | 리소스 이름 접두사 | string | - | yes |
| vpc_cidr | VPC CIDR 블록 | string | - | yes |
| azs | 가용 영역 리스트 | list(string) | - | yes |
| public_subnet_cidrs | Public 서브넷 CIDR | list(string) | - | yes |
| private_subnet_cidrs | Private 서브넷 CIDR | list(string) | - | yes |
| enable_nat | NAT Gateway 활성화 | bool | true | no |
| tags | 리소스 태그 | map(string) | {} | no |

## 출력 값

| 출력 | 설명 |
|------|------|
| vpc_id | VPC ID |
| public_subnet_ids | Public 서브넷 ID 리스트 |
| private_subnet_ids | Private 서브넷 ID 리스트 |
| nat_gateway_ids | NAT Gateway ID 리스트 |
| private_route_table_ids | Private Route Table ID 리스트 |

## 요구사항

- Terraform >= 1.5.0
- AWS Provider >= 5.0
