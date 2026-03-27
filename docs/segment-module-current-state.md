# Segment Module Current State

## Scope
- Package: `com.manage.crm.segment`
- Primary responsibilities:
  - Segment definition CRUD
  - Segment condition validation
  - Segment target user resolution
  - Segment matched-user read API

## Layer Responsibilities

### Controller (`segment.controller`)
- Responsibility: HTTP I/O only.
- Rule:
  - Constructor dependencies are use cases only (`segment.application`).
  - Request validation and response mapping stay in controller.
  - Repository/service direct orchestration is not done here.

### Use Case (`segment.application`)
- Responsibility: single business scenario entrypoint.
- Rule:
  - Bean annotation is `@Component`.
  - Class name ends with `UseCase`.
  - Public entrypoint is `execute(...)`.
  - Input/Output contracts are in `segment.application.dto`.
- Current use cases:
  - `PostSegmentUseCase` (UC-SEGMENT-001)
  - `BrowseSegmentUseCase` (UC-SEGMENT-002)
  - `GetSegmentUseCase` (UC-SEGMENT-003)
  - `DeleteSegmentUseCase` (UC-SEGMENT-004)
  - `GetSegmentMatchedUsersUseCase` (UC-SEGMENT-005)

### Service (`segment.service`)
- Responsibility: reusable orchestration and evaluation logic across use cases/modules.
- Current service:
  - `SegmentTargetingServiceImpl`
- Service behavior:
  - Resolves matched user ids by applying ordered segment conditions.
  - Supports campaign-scoped evaluation and global evaluation.
  - Coordinates segment/event/user repositories.

### Domain (`segment.domain`)
- Responsibility: segment aggregates, condition rules, repository contracts.
- Current rule objects:
  - `SegmentOperator`
  - `SegmentValueType`
- Repository convention:
  - Query method names explicitly include ordering and filter intent.

## DTO/Mapping State
- Segment DTO transformation is centralized in `segment.application.dto` extensions.
- Shared mappings:
  - `Segment -> SegmentDto`
  - `SegmentCondition -> SegmentConditionDto`
  - `SegmentConditionDto -> PostSegmentConditionIn`

## Conventions Enforced by Tests
- `SegmentConventionTest`
  - Controllers depend only on `segment.application`.
  - Services coordinate multiple collaborators.
  - Application beans are `@Component`, `*UseCase`, and expose `execute(...)`.
  - Dependency direction guards:
    - `application` must not depend on `controller`
    - `service` must not depend on `application/controller`
- `SegmentArchitectureKonsistTest`
  - `*UseCase` location/name and application annotation consistency.

## Operational Baseline
- Validation commands:
  - `./gradlew ktlintFormat`
  - `./gradlew test --tests "*segment*"`
