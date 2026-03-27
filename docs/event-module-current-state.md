# Event Module Current State

## Scope
- Package: `com.manage.crm.event`
- Primary responsibilities:
  - Event ingestion and property-based search
  - Campaign CRUD and segment linking
  - Campaign dashboard metrics and analytics
  - Real-time dashboard stream ingestion/consumption

## Layer Responsibilities

### Controller (`event.controller`)
- Responsibility: HTTP I/O only.
- Rule:
  - Constructor dependencies are use cases only (`event.application`).
  - Request parsing and response DTO mapping stay here.
  - Domain repository/cache/stream managers are not injected directly.

### Use Case (`event.application`)
- Responsibility: single business scenario entrypoint.
- Rule:
  - Public entrypoint is `execute(...)`.
  - Class name ends with `UseCase`.
  - Input/Output contracts are defined in `event.application.dto`.
  - If intent is not obvious from method structure, UC comment is required.

### Service (`event.service`)
- Responsibility: cross-component collaboration (multiple repositories/services/managers).
- Current services:
  - `CampaignEventsService`: campaign-event 조회 조합 책임
  - `CampaignDashboardMetricsService`: 스트림 이벤트 기반 메트릭 적재 책임
- Rule:
  - “공통 함수 묶음” 목적의 서비스는 만들지 않음.
  - 순수 계산 로직은 util로 분리.

### Stream Manager (`event.stream`)
- Responsibility: Redis Stream registry/read/write/trim 같은 인프라 접근.
- Current managers:
  - `CampaignDashboardStreamManager`
  - `CampaignStreamRegistryManager`
- Rule:
  - 비즈니스 시나리오 판단은 하지 않고, 인프라 동작만 제공.

### Repository (`event.domain.repository`)
- Responsibility: persistence query contract.
- Current convention:
  - 메서드명은 조건을 포함해 명확히 (`findEventIdsByCampaignIdAndCreatedAtRange`).
  - 반복 SQL 조건은 상수로 재사용해 쿼리 일관성 유지.
  - 시간 범위는 `[start, end)` 규칙 사용 (`>= start`, `< end`).

## Current Use Cases
- `PostEventUseCase` (UC-EVENT-001): 이벤트 적재 + 캠페인 연결 + 스트림 발행
- `PostCampaignUseCase` (UC-CAMPAIGN-001): 캠페인 생성
- `ListCampaignsUseCase` (UC-CAMPAIGN-002): 캠페인 목록 조회
- `GetCampaignUseCase` (UC-CAMPAIGN-003): 캠페인 상세 조회
- `UpdateCampaignUseCase` (UC-CAMPAIGN-004): 캠페인 수정
- `DeleteCampaignUseCase` (UC-CAMPAIGN-005): 캠페인 삭제
- `StreamCampaignDashboardUseCase` (UC-CAMPAIGN-006): 대시보드 스트림 열기
- `GetCampaignDashboardUseCase` (UC-CAMPAIGN-007): 대시보드 메트릭 조회
- `GetCampaignSummaryUseCase` (UC-CAMPAIGN-008): 요약 지표 조회
- `GetCampaignFunnelAnalyticsUseCase` (UC-CAMPAIGN-009): 퍼널 분석
- `GetCampaignSegmentComparisonUseCase` (UC-CAMPAIGN-010): 세그먼트 비교 분석
- `GetCampaignDashboardStreamStatusUseCase` (UC-CAMPAIGN-011): 스트림 길이 조회
- Existing:
  - `BrowseEventsUseCase`
  - `SearchEventsUseCase`

## Conventions Enforced by Tests
- Test: `EventConventionTest`
- Checked rules:
  - Controllers depend only on `event.application` classes.
  - Services in `event.service` coordinate multiple collaborators.
  - All `*UseCase` classes expose `execute(...)`.

## Pure Utility Rules
- Shared pure calculations are extracted to util.
- Current example:
  - `event.application.util.toPercentage(...)`

## Operational Quality Gates
- Build gates used before PR:
  - `./gradlew compileKotlin`
  - `./gradlew compileTestKotlin`
  - `./gradlew test`
  - `./gradlew ktlintFormat`
