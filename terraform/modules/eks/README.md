# AWS EKS Module

Amazon EKS 클러스터와 노드 그룹을 생성하는 Terraform 모듈입니다.

## 기능

- EKS Control Plane 생성
- Managed Node Group 구성
- IAM 역할 및 정책 자동 생성
- 보안 그룹 구성
- CloudWatch 로깅 설정

## 사용 예시

```hcl
module "eks" {
  source = "../../modules/eks"

  cluster_name    = "crm-dev"
  cluster_version = "1.29"
  
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  public_subnet_ids  = module.networking.public_subnet_ids
  
  enable_cluster_public_access = true
  cluster_public_access_cidrs  = ["0.0.0.0/0"]
  
  node_group_instance_types   = ["t3.medium"]
  node_group_desired_capacity = 2
  node_group_min_size         = 1
  node_group_max_size         = 5
  
  tags = {
    Environment = "dev"
    Project     = "crm"
  }
}
```

## 입력 변수

| 변수 | 설명 | 타입 | 기본값 | 필수 |
|------|------|------|--------|------|
| cluster_name | EKS 클러스터 이름 | string | - | yes |
| cluster_version | Kubernetes 버전 | string | "1.29" | no |
| vpc_id | VPC ID | string | - | yes |
| private_subnet_ids | Private 서브넷 ID | list(string) | - | yes |
| public_subnet_ids | Public 서브넷 ID | list(string) | - | yes |
| node_group_instance_types | 노드 인스턴스 타입 | list(string) | ["t3.medium"] | no |
| node_group_desired_capacity | 원하는 노드 수 | number | 2 | no |
| node_group_min_size | 최소 노드 수 | number | 1 | no |
| node_group_max_size | 최대 노드 수 | number | 5 | no |

## 출력 값

| 출력 | 설명 |
|------|------|
| cluster_id | EKS 클러스터 ID |
| cluster_endpoint | EKS 클러스터 엔드포인트 |
| cluster_certificate_authority_data | CA 인증서 데이터 |
| cluster_security_group_id | 클러스터 보안 그룹 ID |

## 요구사항

- Terraform >= 1.5.0
- AWS Provider >= 5.0
