# Terraform Modules

## 핵심 모듈

| 모듈 | 설명 |
|------|------|
| `networking` | VPC, subnet, route, NAT gateway |
| `eks` | EKS cluster, node group, OIDC provider |
| `ecr` | 컨테이너 이미지 저장소 |
| `rds` | PostgreSQL database |
| `elasticache` | Redis replication group |
| `aws-secrets-manager` | 애플리케이션 시크릿 저장 |
| `aws-app-runtime` | 앱이 직접 사용하는 AWS 리소스 구성 |
| `msk` | Kafka cluster |

## aws-app-runtime

`aws-app-runtime` 모듈은 애플리케이션이 직접 참조하는 AWS 리소스를 한 번에 생성합니다.

- cache invalidation SNS topic
- cache invalidation SQS queue + subscription
- schedule target SQS queue
- EventBridge Scheduler group
- EventBridge Scheduler execution role
- SES configuration set
- runtime IAM user and access key

대표 출력값:

- `cache_invalidation_topic_arn`
- `schedule_queue_arn`
- `schedule_group_name`
- `schedule_role_arn`
- `ses_configuration_set_name`
- `runtime_access_key_id`
- `runtime_secret_access_key`

## rds

`rds` 모듈은 PostgreSQL 인스턴스를 생성합니다.

- private subnet 배치
- security group 생성
- EKS node security group ingress 허용
- parameter group, backup, deletion protection 설정 지원

대표 출력값:

- `db_instance_endpoint`
- `db_instance_address`
- `db_instance_port`
- `db_instance_name`
- `security_group_id`

## elasticache

`elasticache` 모듈은 Redis replication group를 생성합니다.

- private subnet 배치
- security group 생성
- EKS node security group ingress 허용
- cluster mode, auth token, encryption 설정 지원

대표 출력값:

- `primary_endpoint_address`
- `reader_endpoint_address`
- `configuration_endpoint_address`
- `port`
- `security_group_id`

## 환경 연결 방식

각 AWS 환경은 아래 흐름으로 모듈을 연결합니다.

1. `networking`
2. `eks`
3. `ecr`
4. `aws-app-runtime`
5. `aws-secrets-manager`
6. `rds`
7. `elasticache`
8. 필요 시 `msk`
