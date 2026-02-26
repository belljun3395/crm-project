# CRM 운영 콘솔 기능 가이드

## 문서 정보
- 기준 브랜치: `main`
- 기준일: 2026-02-26
- 대상: CRM 운영 콘솔(프론트엔드) 사용자 및 운영 담당자
- 작성 근거: 최근 기능 이슈/PR(`#185~#199`, `#214~#225`, `#227`, `#228`) + 현재 FE/API 구현

## 1. 이 문서의 목적
- 운영 콘솔 화면별 기능을 빠르게 이해할 수 있게 설명합니다.
- 실제 API/입력 규칙/오류 조건까지 포함한 운영 명세를 제공합니다.
- "어디서 무엇을 눌러 어떤 결과를 확인하는지"를 업무 순서 중심으로 안내합니다.

## 2. 전체 기능 맵
- 개요: Overview, Campaign Dashboard, Audit Logs
- 고객/행동: Users, Events, Segments, Journeys, Actions
- 메시지 운영: Templates, Email Histories, Email Schedules
- 외부 연동: Webhooks

## 3. 공통 운영 규칙
### 3.1 Idempotency-Key(중복 요청 방지)
다음 쓰기 API 호출에는 `Idempotency-Key` 헤더가 자동 부여됩니다.
- `POST /users`
- `POST /events`
- `POST /events/campaign`
- `POST /emails/send/notifications`
- `POST /emails/schedules/notifications/email`
- `POST /webhooks`
- `PUT /webhooks/{id}`
- `POST /segments`
- `PUT /segments/{id}`
- `POST /journeys`
- `POST /actions/dispatch`

적용 근거: Issue `#196`, PR `#215`

### 3.2 조회/검색 화면 기본 동작
- 대부분 화면은 최초 진입 시 목록을 자동 조회합니다.
- 검색/필터는 빈 값 허용 여부가 화면마다 다릅니다.
- 잘못된 입력(예: JSON 형식 오류, 양수 ID 조건 위반)은 화면에서 즉시 검증합니다.

### 3.3 시간/날짜 처리
- 화면 표시 시간은 브라우저 로컬 시간대로 렌더링됩니다.
- datetime 입력은 `YYYY-MM-DDTHH:mm` 형식이며 서버 전송 시 초(`:00`)가 보정될 수 있습니다.

## 4. 화면별 상세 명세

## 4.1 Overview
### 목적
- 전체 기능 상태를 KPI 카드와 최근 이력으로 빠르게 파악합니다.

### 주요 기능
- 카드 지표: 사용자/템플릿/스케줄/이메일 이력/웹훅/세그먼트/여정/여정 실행/액션/감사로그 수량
- Recent Users: 최근 사용자 목록
- Recent Action Dispatches: 최근 액션 전송 이력

### 연결 API
- `GET /users`
- `GET /users/count`
- `GET /emails/templates`
- `GET /emails/histories`
- `GET /emails/schedules/notifications/email`
- `GET /webhooks`
- `GET /segments`
- `GET /journeys`
- `GET /journeys/executions`
- `GET /actions/dispatch/histories`
- `GET /audit-logs`

## 4.2 Campaign Dashboard
### 목적
- 캠페인 성과 메트릭과 실시간 이벤트 스트림(SSE)을 운영 관점에서 확인합니다.

### 주요 기능
- 기간 필터(`MINUTE/HOUR/DAY/WEEK/MONTH`, 시작/종료 시각)
- Summary 지표(전체/24시간/7일 이벤트 수)
- Stream 상태 조회(길이)
- SSE 연결/해제/재연결 상태 표시
- 실시간 이벤트 리스트 최대 50건 유지

### 입력/검증
- Campaign ID는 양수 정수 필수
- SSE duration은 양수 값만 허용(잘못된 값은 3600초 기본값 적용)

### 연결 API
- `GET /campaigns/{campaignId}/dashboard`
- `GET /campaigns/{campaignId}/dashboard/summary`
- `GET /campaigns/{campaignId}/dashboard/stream/status`
- `GET /campaigns/{campaignId}/dashboard/stream` (EventSource)

### 근거
- Issue `#197`, `#192`
- PR `#214`, `#216`, `#217`

## 4.3 Webhooks
### 목적
- 외부 시스템 이벤트 전송 엔드포인트를 등록/수정/삭제하고 전달 품질을 모니터링합니다.

### 주요 기능
- Webhook CRUD
- 이벤트 타입 다중 등록(쉼표 구분)
- 활성/비활성 제어
- Delivery Logs 조회
- Dead Letters(DLQ) 조회

### 입력/검증
- URL은 `http://` 또는 `https://`만 허용
- 이벤트 타입 최소 1개 이상 필수

### 연결 API
- `POST /webhooks`
- `PUT /webhooks/{id}`
- `DELETE /webhooks/{id}`
- `GET /webhooks`
- `GET /webhooks/{id}`
- `GET /webhooks/{id}/deliveries`
- `GET /webhooks/{id}/dead-letters`

### 근거
- Issue `#189`, `#197`, `#190`
- PR `#214`, `#219`, `#220`, `#221`

## 4.4 Users
### 목적
- 고객 식별자(externalId)와 속성(JSON 문자열)을 등록하고 조회합니다.

### 주요 기능
- 사용자 등록(Enroll)
- externalId/attributes 텍스트 검색
- 사용자 수량 지표 조회

### 연결 API
- `GET /users`
- `POST /users`
- `GET /users/count`

## 4.5 Events
### 목적
- 이벤트를 적재하고 조건 DSL 기반으로 이벤트를 조회합니다.

### 주요 기능
- 이벤트 생성(이벤트명, externalId, campaignName, property)
- 서버 검색(Event Name + Where)
- 로컬 결과 필터링

### 입력/검증
- 검색 시 `eventName`, `where` 둘 다 필수
- 생성 시 `name`, `externalId` 필수
- where 예시
  - `category&electronics&=&end`
  - `category&electronics&=&and,brand&samsung&=&end`
  - `amount&100&amount&200&between&end`

### 연결 API
- `GET /events?eventName&where`
- `POST /events`

### 근거
- Issue `#194`, `#191`
- PR `#218`

## 4.6 Email Templates
### 목적
- 재사용 가능한 메일 템플릿(제목/본문/변수/버전)을 관리합니다.

### 주요 기능
- 템플릿 목록 조회
- 템플릿 생성
- 템플릿 삭제

### 연결 API
- `GET /emails/templates`
- `POST /emails/templates`
- `DELETE /emails/templates/{templateId}`

### 변수 체계 근거
- Issue `#199`
- PR `#202`

## 4.7 Email Histories
### 목적
- 발송 결과(성공/실패) 이력을 페이징으로 조회합니다.

### 주요 기능
- userId/sendStatus/page/size 필터
- total/page/size 표시
- 이메일 메시지 ID 추적

### 연결 API
- `GET /emails/histories`

## 4.8 Email Schedules
### 목적
- 예약 발송 작업을 등록하고 만료 시점/취소를 관리합니다.

### 주요 기능
- 템플릿 + 사용자 집합 기반 스케줄 생성
- 등록된 스케줄 목록 확인
- 스케줄 취소

### 입력/검증
- templateId, userIds, expiredTime 필수
- userIds는 쉼표 구분 숫자 목록

### 연결 API
- `GET /emails/schedules/notifications/email`
- `POST /emails/schedules/notifications/email`
- `DELETE /emails/schedules/notifications/email/{scheduleId}`

## 4.9 Segments
### 목적
- 조건 DSL(JSON) 기반 고객 그룹을 생성/수정/삭제합니다.

### 주요 기능
- Segment CRUD
- 활성 상태 토글
- 조건 건수/생성시각 확인

### 입력/검증
- 이름 필수
- conditions JSON은 배열이며 최소 1개 조건 필요
- 각 조건은 `field`, `operator`, `valueType`, `value` 필수

### 연결 API
- `GET /segments`
- `GET /segments/{id}`
- `POST /segments`
- `PUT /segments/{id}`
- `DELETE /segments/{id}`

### 근거
- Issue `#188`
- PR `#222`

## 4.10 Journeys
### 목적
- 이벤트/세그먼트 트리거 기반 자동화 흐름(여정)을 구성하고 실행 이력을 조회합니다.

### 주요 기능
- Journey 생성
- 실행(Execution) 목록 조회
- 실행 히스토리(스텝별 상태/재시도) 조회

### 입력/검증
- Journey name 필수
- steps JSON은 배열 + `stepOrder(양수)` + `stepType(공백 불가)` 필수
- triggerSegmentId는 입력 시 양수

### 연결 API
- `GET /journeys`
- `POST /journeys`
- `GET /journeys/executions`
- `GET /journeys/executions/{executionId}/histories`

### 근거
- Issue `#187`
- PR `#225`

## 4.11 Actions
### 목적
- Email/Slack/Discord 채널로 즉시 메시지를 전송하고 결과를 추적합니다.

### 주요 기능
- 채널 선택 전송
- 변수 JSON 치환
- 전송 이력 조회(캠페인/여정 연계 가능)

### 입력/검증
- destination, body 필수
- variables는 JSON object 형식
- campaignId/journeyExecutionId 입력 시 양수

### 연결 API
- `POST /actions/dispatch`
- `GET /actions/dispatch/histories`

### 근거
- Issue `#185`
- PR `#224`

## 4.12 Audit Logs
### 목적
- 운영 변경 작업(Webhook 쓰기 등)의 감사 추적 로그를 조회합니다.

### 주요 기능
- limit/action/resourceType/actorId 필터
- 상태 코드 및 시각 기반 원인 추적

### 연결 API
- `GET /audit-logs`

### 근거
- Issue `#190`
- PR `#221`

## 5. 운영 추천 사용 순서(실무)
- 1) Users 등록/검증
- 2) Events 적재 및 검색 DSL 검증
- 3) Segments 생성
- 4) Templates 작성
- 5) Actions 즉시 발송으로 채널 점검
- 6) Journeys 구성 후 실행 이력 확인
- 7) Campaign Dashboard로 실시간/집계 지표 확인
- 8) Webhooks 전달 로그 + DLQ 확인
- 9) Audit Logs로 최종 운영 추적

## 6. 테스트 데이터 주입(선택)
- `bash scripts/test-data/seed-all.sh`
- 세부 스크립트: `scripts/test-data/README.md`

근거: PR `#228`

## 7. 알려진 범위/제약
- 개인정보 보존/삭제 정책(Retention/Masking/Right-to-delete)은 아직 진행 중입니다.
- 트리거/조건 DSL은 백엔드 계약을 따르므로 운영 전 샘플 데이터 검증이 필요합니다.

근거: Issue `#198`(OPEN)
