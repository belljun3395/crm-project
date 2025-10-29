# Terraform AWS 스캐폴드

이 디렉터리는 CRM 프로젝트를 AWS에 배포하기 위한 기본 Terraform 구성을 제공합니다.  
VPC · EKS · ECR을 모듈화했으며, 추후 GCP 구성을 추가하기 쉽게 디렉터리 구조를 분리했습니다.

## 디렉터리 구조
```
terraform/
├── modules/
│   ├── networking          # AWS VPC, 서브넷, NAT
│   ├── eks                 # AWS EKS 클러스터와 노드 그룹
│   ├── ecr                 # AWS ECR 레포지토리
│   ├── aws-secrets-manager # AWS Secrets Manager secret 관리
│   ├── gcp-networking      # GCP VPC, 서브넷, Cloud NAT
│   ├── gke                 # GCP GKE 클러스터와 노드 풀
│   └── artifact_registry   # GCP Artifact Registry
└── environments/
    ├── aws/
    │   └── dev             # AWS 개발 환경 예제
    └── gcp/
        └── dev             # GCP 개발 환경 예제
```

## 선행 조건
- Terraform >= 1.5
- Terraform Cloud 워크스페이스 또는 직접 관리하는 원격 상태 저장소  
  → Terraform Cloud를 사용할 경우 `backend.tf`에 조직/워크스페이스 이름을 입력하고 `terraform login`으로 토큰을 발급받습니다.  
  → 자체 S3+DynamoDB, GCS 등을 사용하려면 backend 구성을 원하는 리소스로 교체하세요.
- AWS: AWS CLI와 EKS/VPC/EC2/IAM/ECR을 생성할 수 있는 IAM 사용자/역할  
- GCP: `gcloud` CLI와 Compute Engine, Kubernetes Engine, Artifact Registry 권한을 가진 서비스 계정(JSON 키) 또는 사용자

## 빠른 시작

### AWS (EKS)
1. `terraform/environments/aws/dev/backend.tf`에서 상태 저장소 정보를 채웁니다.
2. `terraform/environments/aws/dev/terraform.tfvars.example`를 복사해 `terraform.tfvars`로 저장하고, CIDR, 클러스터 이름, 노드 스펙, `app_env` 내 필수 환경 변수 값을 실 환경에 맞게 입력합니다. 입력된 값은 AWS Secrets Manager의 하나의 JSON 시크릿으로 저장됩니다.
3. 작업 디렉터리를 `terraform/environments/aws/dev`로 이동한 뒤 아래 명령을 실행합니다.
   ```bash
   terraform init
   terraform plan
   terraform apply
   ```
4. 배포 후 출력되는 `cluster_endpoint`, `cluster_certificate_authority_data`를 이용해 `aws eks update-kubeconfig --name <클러스터명> --region <리전>` 명령으로 `kubectl` 접근을 설정합니다.

### GCP (GKE)
1. `terraform/environments/gcp/dev/backend.tf`에 Terraform Cloud 조직/워크스페이스를 입력합니다.
2. `terraform/environments/gcp/dev/terraform.tfvars.example`을 복사해 `terraform.tfvars`로 저장하고 프로젝트 ID, 네트워크 CIDR, 노드 스펙을 수정합니다.
3. 서비스 계정 JSON을 준비한 뒤 환경 변수(`GOOGLE_APPLICATION_CREDENTIALS`) 또는 `gcloud auth application-default login`으로 인증합니다.
4. `terraform/environments/gcp/dev` 디렉터리에서 아래 명령을 수행합니다.
   ```bash
   terraform init
   terraform plan
   terraform apply
   ```
5. 출력된 `gke_cluster_endpoint`, `gke_cluster_ca_certificate`를 사용해 `gcloud container clusters get-credentials <클러스터명> --region <리전> --project <프로젝트ID>`로 `kubectl` 컨텍스트를 등록합니다.

## 구성 요소 설명
- **modules/networking (AWS)**  
  - 지정한 AZ에 퍼블릭/프라이빗 서브넷을 생성하고 NAT Gateway를 선택적으로 구성합니다.  
  - `create_flow_logs`를 true로 설정하면 VPC Flow Log를 활성화할 수 있습니다. CloudWatch Logs를 쓸 경우 IAM 역할을 별도로 부여해야 합니다.
- **modules/eks**  
  - 제어 플레인 IAM 역할, 보안 그룹, EKS 클러스터, 기본 노드 그룹(t3.medium, 1~3대)을 정의합니다.  
  - OIDC 프로바이더를 자동 생성하여 IRSA(ServiceAccount 기반 IAM 연동)를 준비합니다.
- **modules/ecr**  
  - 애플리케이션 이미지를 저장할 ECR 레포지토리를 만듭니다. 필요하면 `lifecycle_policy_path`로 이미지 정리 정책을 연결하세요.
- **modules/aws-secrets-manager**  
  - 애플리케이션이 필요로 하는 환경 변수를 AWS Secrets Manager에 JSON 시크릿으로 저장합니다. `app_env` 필수 항목과 `additional_secret_values`를 병합하며, KMS 키나 복구 기간 등을 옵션으로 지정할 수 있습니다.
- **modules/gcp-networking**  
  - Custom VPC, GKE용 서브넷, Pod/Service Secondary Range, Cloud NAT를 생성합니다.  
  - `labels`에 `enable_flow_logs = "true"`를 추가하면 서브넷 Flow Log를 활성화합니다.
- **modules/gke**  
  - Workload Identity, Managed Prometheus, Private Node 옵션을 포함한 GKE Regional 클러스터와 기본 노드 풀을 배포합니다.
- **modules/artifact_registry**  
  - 컨테이너 이미지를 저장할 Artifact Registry 레포지토리를 만듭니다.

## 필수 환경 변수 (AWS 애플리케이션)
`app_env` 블록에는 아래 항목을 모두 채워야 하며, Terraform이 Secrets Manager에 JSON 구조로 저장합니다. 런타임에서는 해당 시크릿을 읽어 환경 변수로 주입해야 합니다.

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `REDIS_HOST`, `REDIS_MAX_REDIRECTS`, `REDIS_PASSWORD`, `REDIS_NODES`
- `MAIL_USERNAME`, `MAIL_PASSWORD`
- `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_CONFIGURATION_SET`
- `AWS_SCHEDULE_ROLE_ARN`, `AWS_SCHEDULE_SQS_ARN`, `AWS_SCHEDULE_GROUP_NAME`
- `KAFKA_BOOTSTRAP_SERVERS`
- `SCHEDULER_PROVIDER` (`aws` 또는 `redis-kafka`)

## 주의 사항
- NAT Gateway는 시간당 요금이 발생하므로, 개발/학습 목적이라면 끄거나 스케줄링을 고려하세요.
- VPC CIDR이 사내망 또는 다른 클라우드와 겹치지 않도록 사전에 조정해야 이후 피어링이 수월합니다.
- EKS 컨트롤 플레인 로그를 3종(api, audit, authenticator) 활성화했으므로 CloudWatch 비용과 로그 보존 정책을 확인하세요.
- 배포 후에는 Kubernetes 애드온(CoreDNS, VPC CNI, kube-proxy)과 모니터링 스택을 최신 버전으로 유지해야 합니다.
- GKE 프라이빗 노드를 사용할 경우 Cloud NAT 비용과 Control Plane 접근용 Bastion/Identity-Aware Proxy 전략을 함께 고려해야 합니다.

## 다음 단계 아이디어
- Route53, ACM을 추가해 애플리케이션용 도메인/인증서를 자동화합니다.
- Secrets Manager 시크릿을 rotation Lambda 또는 Vault와 연동해 자동 교체/감사를 구성합니다.
- Terraform Cloud 또는 Atlantis를 활용해 PR 기반 Plan/Apply 파이프라인을 구성합니다.
