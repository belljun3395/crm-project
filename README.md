# CRM Project

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/belljun3395/crm-project)

CRM API 서버와 운영 콘솔로 구성된 프로젝트입니다.
주요 도메인은 `User`, `Email`, `Event`, `Webhook`, `Campaign Dashboard` 입니다.

## Repository Structure

- `backend/`: Spring Boot 3 + Kotlin + WebFlux + R2DBC
- `frontend/`: React 기반 운영 콘솔
- `k8s/local/`: minikube 기반 로컬 Kubernetes 개발 환경
- `terraform/`: AWS 인프라 코드
- `docs/`: OpenAPI 산출물
- `scripts/`: 시드 데이터, OpenAPI 갱신, 보조 스크립트

## Local Development

모든 명령은 저장소 루트 기준입니다.

### 1. 로컬 인프라 준비

```bash
./k8s/local/scripts/setup.sh
```

### 2. 애플리케이션 개발 시작

```bash
minikube start
eval $(minikube docker-env)
skaffold dev
```

기본 접속 주소:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

자세한 내용은 [k8s/local/README.md](./k8s/local/README.md)를 참고하세요.

## Test

### Backend

```bash
cd backend
./gradlew test
```

### Frontend

```bash
cd frontend
npm run test:ci
```

## OpenAPI

```bash
cd backend
./gradlew generateOpenApiDocs
cp ./build/openapi.json ../docs/openapi.json
```

또는:

```bash
bash scripts/refresh-openapi-docs.sh
```

## Seed Test Data

백엔드가 `http://localhost:8080`에서 실행 중일 때:

```bash
bash scripts/test-data/seed-all.sh
```

개별 기능 시드는 [scripts/test-data/README.md](./scripts/test-data/README.md)를 참고하세요.
