# CRM 로컬 Kubernetes 개발 환경 가이드

이 디렉터리는 minikube 기반의 로컬 Kubernetes 개발 환경을 제공합니다.

## 구성 요소

```text
namespace: crm-local

- crm-mysql
- crm-database (MySQL alias service)
- crm-redis
- crm-kafka
- crm-localstack
- crm-backend
- crm-frontend
- crm-adminer
- crm-kafka-ui
- crm-redis-insight
```

백엔드는 `SPRING_PROFILES_ACTIVE=k8s`로 실행됩니다.

## 사전 준비

```bash
brew install minikube helm skaffold
```

확인 명령:

```bash
minikube version
helm version
skaffold version
docker info
```

## 최초 설정

프로젝트 루트에서 실행합니다.

```bash
./k8s/local/scripts/setup.sh
```

스크립트가 수행하는 작업:

1. minikube 시작
2. Helm repository 등록
3. `crm-local` namespace 생성
4. MySQL init ConfigMap 적용
5. backend ConfigMap / Secret 적용
6. `crm-database` alias service 적용
7. MySQL, Redis, Kafka 설치
8. LocalStack, Adminer, Kafka UI, Redis Insight 적용

## 개발 시작

```bash
minikube start
eval $(minikube docker-env)
skaffold dev
```

`skaffold dev`는 backend와 frontend 이미지를 빌드하고 배포하며 포트포워드를 함께 실행합니다.

## 서비스 접속

| 서비스 | 주소 |
|--------|------|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Adminer | http://localhost:18080 |
| Redis Insight | http://localhost:18081 |
| Kafka UI | http://localhost:18082 |
| LocalStack | http://localhost:4566 |
| MySQL | localhost:13306 |
| Kafka | localhost:9092 |

## 애플리케이션 기본 연결 정보

### MySQL

- host: `crm-database`
- database: `crm`
- app user: `crm-local`
- app password: `crm-local`

Adminer 접속용 root 계정:

- user: `root`
- password: `root`

### Redis

- password: `password`
- cluster nodes:
  - `crm-redis-0.crm-redis-headless.crm-local.svc.cluster.local:6379`
  - `crm-redis-1.crm-redis-headless.crm-local.svc.cluster.local:6379`
  - `crm-redis-2.crm-redis-headless.crm-local.svc.cluster.local:6379`
  - `crm-redis-3.crm-redis-headless.crm-local.svc.cluster.local:6379`
  - `crm-redis-4.crm-redis-headless.crm-local.svc.cluster.local:6379`
  - `crm-redis-5.crm-redis-headless.crm-local.svc.cluster.local:6379`

### LocalStack

LocalStack은 아래 AWS 리소스를 초기화합니다.

- `crm-dr-cache-invalidation-queue-aws`
- `crm_schedule_event_sqs`
- `crm_ses_sqs`
- `cache-invalidation-topic`
- `local-schedule-group`
- `local-configuration-set`
- `LocalEventBridgeSchedulerRole`

## 운영 명령

전체 상태 확인:

```bash
kubectl get all -n crm-local
```

로그 확인:

```bash
kubectl logs -n crm-local deployment/crm-backend -f
kubectl logs -n crm-local deployment/crm-frontend -f
kubectl logs -n crm-local deployment/crm-localstack -f
kubectl logs -n crm-local pod/crm-redis-0 -f
```

Pod 내부 접속:

```bash
kubectl exec -it -n crm-local deployment/crm-backend -- sh
kubectl exec -it -n crm-local statefulset/crm-mysql -- mysql -h crm-database -ucrm-local -pcrm-local crm
kubectl exec -it -n crm-local pod/crm-redis-0 -- redis-cli -c -a password -p 6379
```

인프라 재시작:

```bash
kubectl rollout restart deployment/crm-localstack -n crm-local
kubectl rollout restart statefulset/crm-kafka -n crm-local
```

개발 종료:

```bash
minikube stop
```

환경 삭제:

```bash
./k8s/local/scripts/teardown.sh
minikube delete
```

## 트러블슈팅

Pod 상태 확인:

```bash
kubectl describe pod <pod-name> -n crm-local
```

Redis cluster 상태 확인:

```bash
kubectl get pods -n crm-local -l app.kubernetes.io/name=redis-cluster
kubectl logs -n crm-local job/crm-redis-cluster-create
```

LocalStack 초기화 로그:

```bash
kubectl logs -n crm-local deployment/crm-localstack
kubectl rollout restart deployment/crm-localstack -n crm-local
```

Skaffold 재빌드:

```bash
eval $(minikube docker-env)
skaffold build --no-cache
```

## 메모

현재 로컬 K8s 패키지는 이 워크트리의 애플리케이션 계약에 맞춘 MySQL 기준 환경입니다.
PostgreSQL 전환 브랜치가 반영되면 DB 패키지와 애플리케이션 설정을 함께 맞춰야 합니다.
