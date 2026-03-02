# CRM API Test Data Seed Scripts

서버를 실행한 뒤 기능별 테스트 데이터를 빠르게 만들기 위한 스크립트입니다.

## Prerequisites

- 백엔드 서버 실행 (`http://localhost:8080`)
- `curl`, `jq` 설치

## Environment Variables

- `BASE_URL` (default: `http://localhost:8080/api/v1`)
- `USER_COUNT` (`seed-users.sh`에서만 사용, default: `5`)
- `SEED_TIMEOUT_SECONDS` (default: `20`)
- `JOURNEY_SYNC_WAIT_SECONDS` (`seed-journeys.sh`에서 비동기 실행 대기, default: `1`)
- `CONTINUE_ON_ERROR` (`seed-all.sh`에서만 사용, `1`이면 실패 스크립트가 있어도 계속 진행)

## API Unit Scripts

- `seed-users.sh`: `/users`, `/users/count`
  - 다건 생성 + 일부 사용자 업데이트 + query/page 케이스 포함
- `seed-segments.sh`: `/segments`, `/segments/{id}`, `/segments/{id}/users`
  - 문자열/이벤트/IN 조건 세그먼트 생성 + campaign scope 미리보기 케이스 포함
- `seed-events.sh`: `/events`, `/events/campaign`, `/events` 검색, `/events/all`
  - 단건 이벤트, 캠페인 이벤트, 세그먼트 벌크 이벤트, 커스텀 property 이벤트 케이스 포함
- `seed-emails.sh`: `/emails/templates`, `/emails/send/notifications`, `/emails/schedules/notifications/email`, `/emails/histories`
  - userIds 발송, campaign+segment 발송, 링크 불일치 검증 실패(예상 실패) 케이스 포함
- `seed-webhooks.sh`: `/webhooks`, `/webhooks/{id}`, `/webhooks/{id}/deliveries`, `/webhooks/{id}/dead-letters`
  - 2개 웹훅 생성/업데이트(활성/비활성 전환 포함) 케이스 포함
- `seed-actions.sh`: `/actions/dispatch`, `/actions/dispatch/histories`
  - campaign A/B + no campaign 발송 케이스 포함
- `seed-journeys.sh`: `/journeys`, `/journeys/executions`, `/journeys/executions/{executionId}/histories`
  - EVENT 트리거 1개 + SEGMENT 트리거 5종(ENTER/EXIT/UPDATE/COUNT_REACHED/COUNT_DROPPED) 생성
  - 유저 속성 변경으로 세그먼트 진입/이탈/수정/임계치 교차를 순차적으로 발생시켜 실행 이력 데이터 생성
- `seed-campaign-dashboard.sh`: `/campaigns/{campaignId}/dashboard`, `/summary`, `/stream/status`
  - 캠페인 2개와 사용자 2명 기준으로 서로 다른 이벤트 분포 데이터 생성
- `seed-audit-logs.sh`: `/audit-logs` (웹훅 생성/수정/삭제를 통해 감사 로그 생성)
  - actor 2명 기준으로 감사 로그 필터링 검증 가능 데이터 생성

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
