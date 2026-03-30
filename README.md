# CRM Project

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/belljun3395/crm-project)

CRM API 서버(백엔드)와 운영 콘솔(프론트엔드)로 구성된 CRM 프로젝트입니다.
현재 메인 도메인은 `User`, `Email`, `Event`, `Webhook`, `Campaign Dashboard` 입니다.

## Repository Structure

- `backend/`: Spring Boot 3 + Kotlin + WebFlux + R2DBC
- `frontend/`: React 기반 운영 콘솔
- `docs/`: OpenAPI 산출물 (`openapi.json`)
- `scripts/`: 개발 환경 초기화/검증 스크립트
- `resources/crm-local-develop-environment/`: 로컬 docker-compose 환경

## Local Development

모든 명령은 저장소 루트 기준입니다.

### 1. 로컬 의존성 환경 실행

```bash
cd scripts && bash local-develop-env-reset
```

`local-develop-env-reset`는 현재 로컬 DB(MySQL)/LocalStack/PubSub/Kafka 헬스와 init 컨테이너 완료까지 대기한 뒤 종료됩니다.

현재 로컬/Kubernetes 개발 스택은 아직 MySQL 기반입니다. PostgreSQL 전환은 Terraform 인프라 문서와 백엔드 전환 브랜치에서 같이 맞춰야 합니다.

### 2. 백엔드 빌드/실행

```bash
cd backend
./gradlew bootJar
java -jar ./build/libs/crm-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 3. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

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

## Seed Test Data

```bash
bash scripts/test-data/seed-all.sh
```

- API 단위 개별 실행은 `scripts/test-data/README.md`를 참고하세요.
