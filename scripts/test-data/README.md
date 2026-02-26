# CRM API Test Data Seed Scripts

서버를 실행한 뒤 기능별 테스트 데이터를 빠르게 만들기 위한 스크립트입니다.

## Prerequisites

- 백엔드 서버 실행 (`http://localhost:8080`)
- `curl`, `jq` 설치

## Environment Variables

- `BASE_URL` (default: `http://localhost:8080/api/v1`)
- `USER_COUNT` (`seed-users.sh`에서만 사용, default: `5`)
- `SEED_TIMEOUT_SECONDS` (default: `20`)
- `CONTINUE_ON_ERROR` (`seed-all.sh`에서만 사용, `1`이면 실패 스크립트가 있어도 계속 진행)

## API Unit Scripts

- `seed-users.sh`: `/users`, `/users/count`
- `seed-segments.sh`: `/segments`, `/segments/{id}`
- `seed-events.sh`: `/events`, `/events/campaign`, `/events` 검색
- `seed-emails.sh`: `/emails/templates`, `/emails/send/notifications`, `/emails/schedules/notifications/email`, `/emails/histories`
- `seed-webhooks.sh`: `/webhooks`, `/webhooks/{id}`, `/webhooks/{id}/deliveries`, `/webhooks/{id}/dead-letters`
- `seed-actions.sh`: `/actions/dispatch`, `/actions/dispatch/histories`
- `seed-journeys.sh`: `/journeys`, `/journeys/executions`, `/journeys/executions/{executionId}/histories`
- `seed-campaign-dashboard.sh`: `/campaigns/{campaignId}/dashboard`, `/summary`, `/stream/status`
- `seed-audit-logs.sh`: `/audit-logs` (웹훅 생성/수정/삭제를 통해 감사 로그 생성)

## Failure Handling

- 일부 연동성 API(`emails/send/notifications`, `campaigns/*/dashboard`)가 로컬 환경 상태에 따라 실패할 수 있어, 해당 스크립트는 경고를 출력하고 데이터 생성 흐름은 계속 진행합니다.
- 나머지 생성 단계는 실패 시 즉시 중단됩니다.

## Run

### 기능별 개별 실행

```bash
bash scripts/test-data/seed-users.sh
bash scripts/test-data/seed-events.sh
bash scripts/test-data/seed-emails.sh
```

### 전체 일괄 실행

```bash
bash scripts/test-data/seed-all.sh
```

실패가 있어도 끝까지 실행하려면:

```bash
CONTINUE_ON_ERROR=1 bash scripts/test-data/seed-all.sh
```
