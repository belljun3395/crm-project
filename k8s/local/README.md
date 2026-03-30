# CRM 로컬 Kubernetes 개발 환경 가이드

기존 Docker Compose 환경을 대체하는 minikube 기반 로컬 K8s 개발 환경입니다.
프로덕션(EKS/GKE)과 동일한 Kubernetes 워크플로우로 로컬 개발을 진행할 수 있습니다.

현재 이 로컬 스택의 데이터베이스는 MySQL입니다. PostgreSQL 전환은 백엔드 설정과 Helm 차트를 함께 바꿔야 하므로, 아래 문서에는 현재 상태와 전환 갭을 같이 적어둡니다.

## 목차

1. [사전 준비](#1-사전-준비)
2. [환경 구성 개요](#2-환경-구성-개요)
3. [최초 환경 설정](#3-최초-환경-설정)
4. [일상적인 개발 워크플로우](#4-일상적인-개발-워크플로우)
5. [서비스 접속 정보](#5-서비스-접속-정보)
6. [자주 쓰는 명령어](#6-자주-쓰는-명령어)
7. [트러블슈팅](#7-트러블슈팅)
8. [Docker Compose와의 차이점](#8-docker-compose와의-차이점)

---

## 1. 사전 준비

### 필수 도구 설치

```bash
# Homebrew로 한 번에 설치
brew install minikube helm skaffold
```

| 도구 | 버전 | 용도 |
|------|------|------|
| minikube | 최신 | 로컬 Kubernetes 클러스터 |
| helm | 3.x | Database / Redis / Kafka 패키지 관리 |
| skaffold | 최신 | 백엔드/프론트엔드 빌드·배포·포트포워드 자동화 |
| docker | 최신 | minikube Docker driver (이미 설치되어 있어야 함) |

### 설치 확인

```bash
minikube version
helm version
skaffold version
docker info
```

---

## 2. 환경 구성 개요

```
minikube (namespace: crm-local)
│
├── [Helm] crm-mysql          bitnami/mysql 8.x
│         Service: crm-mysql:3306
│
├── [Helm] crm-redis          bitnami/redis-cluster (6노드: 3 master + 3 replica)
│         StatefulSet: crm-redis-0 ~ crm-redis-5
│         Headless Service: crm-redis-headless
│
├── [Helm] crm-kafka          bitnami/kafka KRaft 모드
│         Service: crm-kafka:9092
│
├── crm-localstack            localstack/localstack:3.8
│         SES / SQS / SNS / EventBridge / Scheduler / IAM
│
├── crm-adminer               Database 웹 관리 UI
├── crm-kafka-ui              Kafka 웹 관리 UI
├── crm-redis-insight         Redis 웹 관리 UI
│
├── crm-backend               Spring Boot (profile: k8s)
└── crm-frontend              React + Nginx (백엔드 API 프록시 포함)
```

### Spring 프로파일

백엔드는 `SPRING_PROFILES_ACTIVE=k8s`로 기동됩니다.
설정 파일: `backend/src/main/resources/application-k8s.yml`

### PostgreSQL 전환 갭

이 문서는 현재 로컬 실행 상태를 기준으로 유지하지만, PostgreSQL로 넘어가려면 아래 항목을 같이 바꿔야 합니다.

- `backend/src/main/resources/application-k8s.yml`의 R2DBC URL과 드라이버
- `k8s/local/helm-values/mysql.yaml`을 대체할 PostgreSQL Helm values
- `k8s/local/backend/deployment.yaml`의 DB readiness 대기 조건과 포트
- `k8s/local/mysql-init/configmap.yaml`의 초기화 SQL
- `resources/crm-local-develop-environment/docker-compose.yml`의 DB 서비스와 init script

지금 상태에서는 `crm-mysql`과 Adminer가 로컬 DB 진입점입니다. PostgreSQL 전환 브랜치가 들어오기 전까지는 이 가이드를 현재 런타임 기준 문서로 봐야 합니다.

---

## 3. 최초 환경 설정

> 처음 설정하거나 `minikube delete` 후 재구성할 때 실행합니다.

### 3-1. 인프라 기동 스크립트 실행

```bash
# 프로젝트 루트에서 실행
./k8s/local/scripts/setup.sh
```

스크립트가 자동으로 수행하는 작업:

1. minikube 클러스터 시작 (CPU 4코어, 메모리 8GB)
2. Bitnami Helm 레포지터리 등록
3. `crm-local` 네임스페이스 생성
4. Database init ConfigMap 적용 (현재는 MySQL DB / 유저 초기화 SQL)
5. Database, Redis Cluster, Kafka Helm 설치 (각 서비스 Ready 대기)
6. LocalStack, Adminer, Kafka UI, Redis Insight 매니페스트 적용

소요 시간: **약 5~10분** (이미지 최초 다운로드 시 더 걸릴 수 있음)

### 3-2. 모든 Pod 정상 기동 확인

```bash
kubectl get pods -n crm-local
```

아래와 같이 모든 Pod가 `Running` 상태여야 합니다.

```
NAME                                 READY   STATUS    RESTARTS
crm-adminer-xxx                      1/1     Running   0
crm-kafka-0                          1/1     Running   0
crm-kafka-ui-xxx                     1/1     Running   0
crm-localstack-xxx                   1/1     Running   0
crm-mysql-0                          1/1     Running   0
crm-redis-0                          1/1     Running   0
crm-redis-1                          1/1     Running   0
crm-redis-2                          1/1     Running   0
crm-redis-3                          1/1     Running   0
crm-redis-4                          1/1     Running   0
crm-redis-5                          1/1     Running   0
crm-redis-insight-xxx                1/1     Running   0
```

### 3-3. LocalStack 초기화 확인

LocalStack은 기동 후 `/etc/localstack/init/ready.d/setup-aws-services.sh`를 자동 실행합니다.
SQS, SNS, EventBridge, IAM 등이 정상 생성됐는지 로그로 확인합니다.

```bash
kubectl logs -n crm-local deployment/crm-localstack -f
# "🎉 LocalStack AWS services setup completed!" 메시지 확인
```

---

## 4. 일상적인 개발 워크플로우

### 4-1. 매일 개발 시작 시

```bash
# minikube가 꺼져 있으면 기동
minikube start

# 터미널마다 1회 실행 — minikube 내부 Docker daemon 환경 변수 설정
eval $(minikube docker-env)

# 백엔드/프론트엔드 빌드 + K8s 배포 + 포트포워드 자동 실행
# 소스 파일 변경을 감지해 자동으로 재빌드 / 재배포
skaffold dev
```

> `skaffold dev`를 종료(Ctrl+C)하면 배포된 앱 서비스(backend, frontend)가 자동으로 삭제됩니다.
> 인프라(Database, Redis, Kafka 등)는 유지됩니다.

### 4-2. 백엔드 / 프론트엔드만 재배포

```bash
# 특정 이미지만 빌드 후 배포
skaffold run --image crm-backend
skaffold run --image crm-frontend
```

### 4-3. 인프라 서비스 재시작 (필요 시)

```bash
# LocalStack 재시작
kubectl rollout restart deployment/crm-localstack -n crm-local

# Kafka 재시작
kubectl rollout restart statefulset/crm-kafka -n crm-local
```

### 4-4. 개발 종료

```bash
# skaffold dev를 Ctrl+C로 종료
# minikube 일시정지 (클러스터 데이터 유지)
minikube stop
```

### 4-5. 환경 완전 삭제

```bash
./k8s/local/scripts/teardown.sh

# 클러스터 데이터까지 완전 삭제 시
minikube delete
```

---

## 5. 서비스 접속 정보

`skaffold dev` 실행 시 아래 포트로 자동 포트포워드됩니다.

| 서비스 | 주소 | 비고 |
|--------|------|------|
| **Frontend** | http://localhost:3000 | React 앱 |
| **Backend API** | http://localhost:8080 | Spring Boot |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API 문서 |
| **Adminer** | http://localhost:18080 | Database 웹 관리 |
| **Kafka UI** | http://localhost:18082 | Kafka 웹 관리 |
| **Redis Insight** | http://localhost:18081 | Redis 웹 관리 |
| **LocalStack** | http://localhost:4566 | AWS 서비스 목 |
| **Database** | localhost:13306 | root / root |
| **Kafka** | localhost:9092 | |

> 기존 Docker Compose 포트 번호와 동일하게 맞춰놨습니다.

### Adminer 접속

- 서버: `crm-mysql`
- PostgreSQL 전환 후에는 이 항목을 새 DB 서비스명으로 바꿔야 합니다.
- 사용자: `root`
- 비밀번호: `root`
- 데이터베이스: `crm`

### Redis Insight 접속

Redis Insight UI에서 새 연결 추가:

- Host: `crm-redis-headless.crm-local.svc.cluster.local` (클러스터 내부 접속 시)
- 또는 `kubectl port-forward`로 특정 노드에 직접 접속:

```bash
kubectl port-forward pod/crm-redis-0 -n crm-local 6379:6379
# Redis Insight에서 localhost:6379 + 비밀번호: password
```

### LocalStack AWS 서비스 확인

```bash
# 모든 SQS 큐 목록
aws --endpoint-url=http://localhost:4566 sqs list-queues \
  --region ap-northeast-2 \
  --no-sign-request

# 모든 SNS 토픽 목록
aws --endpoint-url=http://localhost:4566 sns list-topics \
  --region ap-northeast-2 \
  --no-sign-request
```

---

## 6. 자주 쓰는 명령어

### 전체 상태 확인

```bash
kubectl get all -n crm-local
```

### 로그 보기

```bash
# 백엔드
kubectl logs -n crm-local deployment/crm-backend -f

# 프론트엔드
kubectl logs -n crm-local deployment/crm-frontend -f

# LocalStack (AWS 서비스 초기화 확인)
kubectl logs -n crm-local deployment/crm-localstack -f

# Redis Cluster (특정 노드)
kubectl logs -n crm-local pod/crm-redis-0 -f
```

### Pod 내부 접속

```bash
# 백엔드 컨테이너 쉘
kubectl exec -it -n crm-local deployment/crm-backend -- sh

# DB 클라이언트
kubectl exec -it -n crm-local statefulset/crm-mysql -- mysql -uroot -proot crm

# Redis CLI (클러스터 모드)
kubectl exec -it -n crm-local pod/crm-redis-0 -- \
  redis-cli -c -a password -p 6379
```

### Helm 차트 업그레이드

```bash
helm upgrade crm-mysql bitnami/mysql \
  -n crm-local -f k8s/local/helm-values/mysql.yaml

helm upgrade crm-redis bitnami/redis-cluster \
  -n crm-local -f k8s/local/helm-values/redis-cluster.yaml

helm upgrade crm-kafka bitnami/kafka \
  -n crm-local -f k8s/local/helm-values/kafka.yaml
```

### minikube 리소스 사용량 확인

```bash
minikube dashboard         # 브라우저 대시보드 열기
kubectl top pods -n crm-local
kubectl top nodes
```

---

## 7. 트러블슈팅

### Pod가 Pending 상태일 때

```bash
kubectl describe pod <pod-name> -n crm-local
```

주로 minikube 메모리/CPU 부족이 원인입니다.

```bash
# minikube 리소스 확인
minikube status

# 메모리 부족 시 minikube 재구성 (데이터 삭제됨)
minikube delete
minikube start --driver=docker --cpus=4 --memory=10240
```

### Redis Cluster가 초기화되지 않을 때

Redis Cluster는 6개 Pod가 모두 Ready 상태여야 클러스터가 형성됩니다.

```bash
# Pod 상태 확인
kubectl get pods -n crm-local -l app.kubernetes.io/name=redis-cluster

# 클러스터 초기화 Job 로그 확인
kubectl logs -n crm-local job/crm-redis-cluster-create
```

### 백엔드가 Redis에 연결 실패할 때

```bash
# 백엔드 Pod에서 Redis 노드로 직접 연결 테스트
kubectl exec -it -n crm-local deployment/crm-backend -- \
  sh -c "nc -zv crm-redis-0.crm-redis-headless.crm-local.svc.cluster.local 6379"
```

### LocalStack init 스크립트가 실패할 때

```bash
# 로그 확인
kubectl logs -n crm-local deployment/crm-localstack

# Pod 재시작으로 init 재실행
kubectl rollout restart deployment/crm-localstack -n crm-local
```

### Skaffold 빌드 오류 시

```bash
# minikube Docker 환경 재설정
eval $(minikube docker-env)

# 이미지 캐시 정리 후 재빌드
skaffold build --no-cache
```

### minikube Docker 환경을 해제하고 싶을 때

```bash
eval $(minikube docker-env --unset)
```

---

## 8. Docker Compose와의 차이점

| 항목 | Docker Compose | K8s (minikube) |
|------|----------------|-----------------|
| 실행 명령 | `docker compose up` | `setup.sh` → `skaffold dev` |
| Redis 포트 | 7001~7006 (shared network) | 6379 (pod별 고유 IP) |
| Redis connect-ip | `localhost` (필수) | 미설정 (pod IP 직접 라우팅) |
| 앱 재빌드 | `docker compose build` | `skaffold dev` 자동 감지 |
| 로그 확인 | `docker compose logs -f` | `kubectl logs -f` |
| 서비스 접속 | 직접 포트 매핑 | `skaffold dev` 포트포워드 |
| 데이터 유지 | 볼륨 마운트 | PersistentVolume (database) |
| LocalStack SERVICES | `ses,sqs,events,scheduler` | `ses,sqs,sns,events,scheduler,iam` (SNS/IAM 추가) |

> **포트 번호는 Docker Compose와 동일하게 맞춰놨습니다.**
> 기존 로컬 테스트 스크립트나 설정을 그대로 사용할 수 있습니다.
