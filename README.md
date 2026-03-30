# CRM Project

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/belljun3395/crm-project)

CRM API 서버(백엔드)와 운영 콘솔(프론트엔드)로 구성된 CRM 프로젝트입니다.
현재 메인 도메인은 `User`, `Email`, `Event`, `Webhook`, `Campaign Dashboard` 입니다.

## Repository Structure

- `backend/`: Spring Boot 3 + Kotlin + WebFlux + R2DBC(MySQL)
- `frontend/`: React 기반 운영 콘솔
- `docs/`: 아키텍처/플로우/대시보드/OpenAPI 산출물
- `scripts/`: 개발 환경 초기화/검증 스크립트
- `resources/crm-local-develop-environment/`: 로컬 docker-compose 환경

## Local Development

모든 명령은 저장소 루트 기준입니다.

### 1. 로컬 의존성 환경 실행

```bash
cd scripts && bash local-develop-env-reset
```

`local-develop-env-reset`는 MySQL/LocalStack/PubSub/Kafka 헬스와 init 컨테이너 완료까지 대기한 뒤 종료됩니다.

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

## Core Documents

- [문서 인덱스(Current State)](./docs/index.md)
- [API 기능/사용 허브](./docs/CRM_API_CAPABILITY_AND_USAGE.md)
- [아키텍처/구현 허브](./docs/CRM_ARCHITECTURE_AND_IMPLEMENTATION.md)
- [API 현재 상태 요약](./docs/publish/API_CURRENT_STATE.md)
- [API 도메인별 기능/엔드포인트](./docs/publish/API_DOMAIN_CAPABILITIES.md)
- [아키텍처 현재 상태](./docs/publish/ARCHITECTURE_CURRENT_STATE.md)
- [런타임 플로우/운영 제약](./docs/publish/ARCHITECTURE_RUNTIME_FLOWS.md)
- [도메인/유즈케이스 플로우](./docs/Domain-and-UseCase-Flows.md)
- [캠페인 대시보드 구현 문서](./docs/CAMPAIGN_DASHBOARD_IMPLEMENTATION.md)
- [백엔드 대시보드 운영 문서](./backend/docs/campaign-dashboard/README.md)
- [OpenAPI 산출물](./docs/openapi.json)

## Notes

- 쓰기 API 일부는 `Idempotency-Key` 헤더를 요구합니다.
- Webhook 기능은 `webhook.enabled` 설정으로 토글됩니다.
- Flyway 마이그레이션 위치는 `backend/src/main/resources/db/migration/entity` 입니다.

## Seed Test Data

```bash
bash scripts/test-data/seed-all.sh
```

- API 단위 개별 실행은 `scripts/test-data/README.md`를 참고하세요.
