# GCP GKE Module

Google Kubernetes Engine 클러스터를 생성하는 Terraform 모듈입니다.

## 기능

- GKE 클러스터 생성
- Node Pool 구성
- Private Cluster 지원
- Auto-scaling 설정
- Workload Identity 구성

## 사용 예시

```hcl
module "gke" {
  source = "../../modules/gke"

  project_id   = "my-project-id"
  region       = "asia-northeast3"
  zones        = ["asia-northeast3-a", "asia-northeast3-b"]
  cluster_name = "crm-dev-gke"
  
  network    = module.networking.network_name
  subnetwork = module.networking.subnet_name
  
  cluster_secondary_range_pods     = "crm-dev-pods"
  cluster_secondary_range_services = "crm-dev-services"
  
  release_channel        = "REGULAR"
  enable_private_nodes   = true
  enable_private_endpoint = false
  
  node_pool_machine_type  = "e2-standard-4"
  node_pool_disk_size_gb  = 100
  node_pool_min_count     = 1
  node_pool_max_count     = 5
  node_pool_initial_count = 2
  
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
| region | 리전 | string | - | yes |
| cluster_name | GKE 클러스터 이름 | string | - | yes |
| network | VPC 네트워크 이름 | string | - | yes |
| subnetwork | 서브넷 이름 | string | - | yes |
| release_channel | 릴리스 채널 | string | "REGULAR" | no |
| node_pool_machine_type | 노드 머신 타입 | string | "e2-standard-4" | no |
| node_pool_min_count | 최소 노드 수 | number | 1 | no |
| node_pool_max_count | 최대 노드 수 | number | 5 | no |

## 출력 값

| 출력 | 설명 |
|------|------|
| cluster_id | GKE 클러스터 ID |
| cluster_endpoint | GKE 클러스터 엔드포인트 |
| cluster_ca_certificate | CA 인증서 |
| cluster_name | GKE 클러스터 이름 |

## 요구사항

- Terraform >= 1.5.0
- Google Provider >= 5.0
