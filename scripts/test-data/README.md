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
- `JOURNEY_EXECUTION_WAIT_TIMEOUT_SECONDS` (`seed-journeys.sh` 최종 실행 집계 대기 timeout, default: `45`)
- `JOURNEY_EXECUTION_WAIT_INTERVAL_SECONDS` (`seed-journeys.sh` 최종 실행 집계 폴링 간격, default: `1`)
- `CONTINUE_ON_ERROR` (`seed-all.sh`에서만 사용, `1`이면 실패 스크립트가 있어도 계속 진행)

## API Unit Scripts

- `seed-users.sh`: `/users`, `/users/count`
  - 다건 생성 + 일부 사용자 업데이트 + query/page 케이스 포함
- `seed-segments.sh`: `/segments`, `/segments/{id}`, `/segments/{id}/users`
  - 문자열/이벤트/IN 조건 세그먼트 생성 + campaign scope 미리보기 케이스 포함
- `seed-events.sh`: `/events`, `/events/campaign`, `/events` 검색, `/events/all`
  - 단건 이벤트, 캠페인 이벤트, 세그먼트 벌크 이벤트, 커스텀 property 이벤트 케이스 포함
- `seed-campaign-management.sh`: `/campaigns` 목록/상세/생성/수정/삭제
  - 캠페인 CRUD 전체 플로우(생성→목록/상세 확인→수정→삭제) 검증
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
  - 퍼널 기본값(`signup,open,click`)과 세그먼트 비교(`purchase`)를 바로 확인할 수 있는 이벤트 분포 데이터 생성
  - 퍼널/세그먼트 API 조회 결과를 함께 출력해 시드 직후 데이터 유입 여부를 즉시 확인 가능
- `pump-campaign-live-stream.sh`: `/events` (지정 캠페인으로 실시간 이벤트 지속 발생)
  - 대시보드 SSE 연결 상태에서 라이브 이벤트 패널을 채우기 위한 스트림 펌프 스크립트
- `check-campaign-dashboard-stream.sh`: `/campaigns/{campaignId}/dashboard/stream` (SSE 연결 검증)
  - SSE `connected`/`campaign-event` 수신 여부를 자동 점검하고 실패 시 출력 덤프 제공
- `seed-audit-logs.sh`: `/audit-logs` (웹훅 생성/수정/삭제를 통해 감사 로그 생성)
  - actor 2명 기준으로 감사 로그 필터링 검증 가능 데이터 생성

## Failure Handling

- 일부 연동성 API(`emails/send/notifications`, `campaigns/*/dashboard`)가 로컬 환경 상태에 따라 실패할 수 있어, 해당 스크립트는 경고를 출력하고 데이터 생성 흐름은 계속 진행합니다.
- `seed-emails.sh`에는 검증 실패 경로 확인을 위한 의도된 invalid 요청(캠페인-세그먼트 불일치)이 포함되어 있어 경고 로그가 발생할 수 있으며, 출력 결과는 `EXPECTED_FAILURE`로 표시됩니다.
- `seed-campaign-dashboard.sh`에서 `summary.totalEvents` 등 상단 카드 수치는 스케줄러 집계(기본 60초 주기) 이후 반영됩니다. 퍼널/세그먼트/스트림 값은 시드 직후에도 확인됩니다.
- 나머지 생성 단계는 실패 시 즉시 중단됩니다.

## Run

### 기능별 개별 실행

```bash
bash scripts/test-data/seed-users.sh
bash scripts/test-data/seed-events.sh
bash scripts/test-data/seed-campaign-management.sh
bash scripts/test-data/seed-emails.sh
bash scripts/test-data/seed-campaign-dashboard.sh
# live stream demo (campaignId는 seed-campaign-dashboard 출력값 사용)
bash scripts/test-data/pump-campaign-live-stream.sh <campaignId> 30 1
# sse endpoint 검증
bash scripts/test-data/check-campaign-dashboard-stream.sh <campaignId> 8 3 1
```

### 전체 일괄 실행

```bash
bash scripts/test-data/seed-all.sh
```

실패가 있어도 끝까지 실행하려면:

```bash
CONTINUE_ON_ERROR=1 bash scripts/test-data/seed-all.sh
```
