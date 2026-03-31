# Segment Module Architecture State

## 1. Layer Responsibilities

### `com.manage.crm.segment.controller`
- Controllers depend only on `segment.application` use cases.
- Controllers parse request/response contracts and do not orchestrate repositories/services directly.

### `com.manage.crm.segment.application`
- Application beans are use-case entry points.
- Bean annotation convention: `@Component` only (not `@Service`).
- Bean naming convention: every application bean name ends with `UseCase`.
- Use cases expose `execute(...)` entrypoints.
- Shared DTO mapping helpers are managed in `segment.application.dto`.

### `com.manage.crm.segment.service`
- Service layer is reserved for reusable orchestration logic.
- Current service `SegmentTargetingServiceImpl` coordinates segment, user, and event data sources.
- Service methods are documented with responsibility-focused KDoc.

### `com.manage.crm.segment.domain`
- Domain layer holds entity/repository contracts and condition rule enums.
- `SegmentOperator` / `SegmentValueType` are defined in domain and shared by validator/service.

## 2. Current Use Case Boundaries

- `PostSegmentUseCase`: segment create/update + condition replacement + post-commit journey trigger enqueue.
- `BrowseSegmentUseCase`: segment list with grouped/ordered conditions.
- `GetSegmentUseCase`: single segment detail with ordered conditions.
- `DeleteSegmentUseCase`: segment delete (condition rows removed by FK cascade).
- `GetSegmentMatchedUsersUseCase`: matched-user list materialization from targeting result.

## 3. Dependency Direction Guardrails

- Controller constructor dependencies must belong to `segment.application`.
- Application layer must not depend on controller layer.
- Service layer must not depend on application/controller layers.
- Service beans are expected to coordinate multiple collaborators (constructor dependency count >= 2).

## 4. Automated Guardrails

### `SegmentConventionTest`
- Validates controller/use case/service conventions and dependency direction.

### `SegmentArchitectureKonsistTest`
- Validates `*UseCase` placement and application annotation/name conventions.

## 5. Build/Lint/Test Baseline

- `./gradlew ktlintFormat`
- `./gradlew test --tests "*Segment*"`
- `./gradlew test`
- `bash scripts/check-backend-spec-governance.sh`
