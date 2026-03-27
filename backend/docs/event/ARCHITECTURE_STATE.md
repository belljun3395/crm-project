# Event Module Architecture State

## 1. Layer Responsibilities

### `com.manage.crm.event.controller`
- Controllers depend only on `event.application` use cases.
- Controllers map HTTP request/response DTO and do not coordinate repositories directly.

### `com.manage.crm.event.application`
- Application beans are use-case entry points.
- Bean annotation convention: use cases in this package use `@Component` (not `@Service`).
- Bean naming convention: every application bean name ends with `UseCase`.
- Use cases expose a single `execute(...)` entrypoint.
- DTO 매핑 성격의 공통 로직은 `event.application.dto` 확장 함수로 관리한다.

### `com.manage.crm.event.service`
- Service layer is reserved for orchestration that coordinates multiple collaborators.
- A service is expected to have at least two constructor dependencies.
- Single-purpose helpers are not modeled as service by default.

### `com.manage.crm.event.stream`
- Stream-related state handling is separated into dedicated stream beans.
- Naming convention:
  - `@Service`: `*Manager` or `*Publisher`
  - `@Component`: `*Consumer`
- Stream publishing responsibility is centralized in `CampaignEventPublisher`.

### `com.manage.crm.event.domain`
- Domain entities, repository interfaces, and projections stay in `domain`.
- Repository query methods are preferred over in-memory sorting/filtering when equivalent DB query is available.

## 2. Campaign Dashboard/Summary State

- Dashboard summary is read directly in use cases through `CampaignDashboardMetricsRepository`.
- Shared summary transformations are defined as DTO mapping extensions in:
  - `event.application.dto.GetCampaignDashboardUseCaseDto`
  - `event.application.dto.GetCampaignSummaryUseCaseDto`
- No dedicated cross-use-case summary reader bean exists in application layer.

## 3. Campaign Stream State

- `CampaignDashboardStreamManager` handles stream write/read operations and stream lifecycle helpers.
- `CampaignStreamRegistryManager` tracks active campaigns and cursor keys.
- `CampaignDashboardStreamConsumer` is responsible for consumption-cycle trimming policy.
- `DeleteCampaignUseCase` includes stream/cache cleanup for campaign deletion lifecycle consistency.

## 4. Query and Access Conventions

- Recent campaign listing uses repository-level query (`LIMIT`) instead of loading full set and slicing in memory.
- Campaign update flow distinguishes:
  - `segmentIds == null`: keep existing segment mapping
  - `segmentIds != null`: validate and replace mapping
- Cache writes that depend on transaction success run after commit.

## 5. Automated Guardrails

### `EventConventionTest`
- Controller constructor dependencies must belong to `event.application`.
- Services in `event.service` must coordinate multiple collaborators.
- Application beans in `event.application` must:
  - end with `UseCase`
  - use `@Component` only
  - provide `execute(...)` if they are use-case classes
- Dependency direction guardrails:
  - `application` must not depend on `controller`
  - `service` must not depend on `application` or `controller`
  - `stream` must not depend on `application` or `controller`

### `EventArchitectureKonsistTest` (Konsist)
- `*UseCase` classes under `event` must reside in `event.application`.
- `event.application` component beans must end with `UseCase`.
- `event.application` does not declare `@Service` beans.

## 6. Build/Lint/Test Baseline

- Kotlin style formatting is maintained by `./gradlew ktlintFormat`.
- Architecture/convention checks execute in test phase and fail CI on rule violations.
