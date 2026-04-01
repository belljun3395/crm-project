# Journey Convention Re-Architecture Plan

## TL;DR

> **Quick Summary**: Journey 모듈을 event/segment와 동일한 거버넌스 기준으로 재구성하되, 단순 스타일 맞춤이 아닌 구조적 유지보수성(경계 명확화, 책임 분리, 테스트 계층화)을 우선 달성한다.
>
> **Deliverables**:
> - Journey 아키텍처/거버넌스 규칙 + 테스트 체계 정렬
> - UseCase/DTO/Controller/Queue 경계 재설계
> - JourneyAutomationService 책임 분해 + 회귀 보호 테스트 확보
>
> **Estimated Effort**: Large
> **Parallel Execution**: YES - 4 implementation waves + final verification wave
> **Critical Path**: governance baseline → package/contract restructuring → automation decomposition → integration verification

---

## Context

### Original Request
- event, segment 컨벤션은 어느 정도 맞춰졌고, journey에 대해 소스코드+테스트 기준으로 컨벤션 정렬 검토 요청.
- 이후 application만이 아니라 journey의 다른 패키지(domain/controller/queue/repository 포함)까지 확장 검토 요청.
- 유지보수가 쉬운 코드적/패키지적 방향 제시 요청.
- 우선순위는 **구조 재정비 우선**으로 확정.

### Interview Summary
**Key Discussions**:
- Backend 중심으로 정렬 진행.
- UseCase 형태는 `execute(UseCaseIn)` 단일화로 정렬.
- UC 코드 시퀀스는 `UC-JOURNEY-001`부터 시작.
- 빠른 패치보다 구조 재정비를 먼저 수행.

**Research Findings**:
- journey는 event/segment 대비 architecture/governance 테스트 부재.
- journey의 DTO/In/Out 위치와 UseCase 스타일이 event/segment와 불일치.
- `JourneyAutomationService`에 로직 집중도가 매우 높음(대형 클래스).
- `journey.queue` 타입이 타 모듈(event/segment/user)로 직접 전파되어 결합도 상승.
- journey 테스트가 application 일부에 편중되어 계층적 품질 게이트 부족.

### Metis Review
**Identified Gaps (addressed in this plan)**:
- 리팩토링 중 계약/동작 회귀 가능성 → 계약 스냅샷 + 단계별 테스트 게이트 포함.
- 구조개편 중 과도한 범위 확장 위험 → Must NOT Have 및 범위 고정 명시.
- 실행 경로 복잡도 증가 위험 → Wave 기반 의존성 매트릭스와 병렬 그룹 고정.

---

## Work Objectives

### Core Objective
Journey 모듈을 event/segment 수준의 컨벤션과 검증 체계로 정렬하고, 장기 유지보수를 위해 패키지 경계/책임 분리를 명확하게 재설계한다.

### Concrete Deliverables
- Journey architecture/governance 테스트 스위트 신설.
- Journey UseCase + DTO 레이어 표준화 (`execute(UseCaseIn)`, `application/dto`).
- Controller 매핑 책임 분리.
- Queue 계약 경계 정리(외부 노출 최소화).
- Automation 핵심 로직 분해 및 회귀 테스트 보강.

### Definition of Done
- [ ] Journey architecture/governance 테스트가 생성되어 통과한다.
- [ ] Journey UseCase 파일들이 UC-KDoc/execute 시그니처 규칙을 충족한다.
- [ ] DTO가 `journey/application/dto`로 정리되고 controller 인라인 매핑이 축소된다.
- [ ] queue 경계 정리 후 타 모듈이 queue 내부 구현 세부사항에 직접 결합하지 않는다.
- [ ] journey 테스트가 application 중심에서 domain/queue/architecture/governance 레이어까지 확장된다.

### Must Have
- `UC-JOURNEY-001`부터 UC 코드 적용.
- 구조 재정비 우선(단순 네이밍 치환 수준에서 종료 금지).
- source + test 동시 정렬.

### Must NOT Have (Guardrails)
- behavior를 바꾸는 비즈니스 로직 변경(의도된 컨벤션/구조 변경 범위를 넘는 기능 변경) 금지.
- queue provider(kafka/in-memory) 런타임 동작 변경 금지.
- journey 외 무관 모듈의 대규모 리팩토링 금지.
- “일단 통과”를 위한 규칙 비활성화/테스트 우회 금지.

---

## Verification Strategy (MANDATORY)

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed.

### Test Decision
- **Infrastructure exists**: YES
- **Automated tests**: YES (Tests-after)
- **Framework**: Gradle test (Kotest/JUnit 혼합)

### QA Policy
- Every task includes executable QA scenarios (Bash/Playwright/tmux as appropriate).
- Evidence path: `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`
- Backend 중심 검증: `./gradlew test --tests "..."`, 필요 시 모듈 단위 테스트 필터 적용.
- API 계약 회귀는 curl 기반으로 핵심 endpoint를 검증.

---

## Execution Strategy

### Parallel Execution Waves

Wave 1 (Start Immediately — governance baseline + package skeleton)
- T1, T2, T3, T4, T5, T6

Wave 2 (After Wave 1 — contract & usecase standardization)
- T7, T8, T9, T10, T11, T12

Wave 3 (After Wave 2 — automation decomposition)
- T13, T14, T15, T16, T17

Wave 4 (After Wave 3 — integration + regression hardening)
- T18, T19, T20, T21

Wave FINAL (After ALL implementation tasks)
- F1, F2, F3, F4

Critical Path: T1 → T7 → T13 → T18 → F1-F4
Parallel Speedup Target: >= 60%
Max Concurrent Target: 5-6 tasks per wave

### Dependency Matrix
- T1: blocked by none → blocks T7, T8, T9
- T2: blocked by none → blocks T10, T11
- T3: blocked by none → blocks T12
- T4: blocked by none → blocks T13
- T5: blocked by none → blocks T14
- T6: blocked by none → blocks T15
- T7: blocked by T1 → blocks T16
- T8: blocked by T1 → blocks T16
- T9: blocked by T1 → blocks T17
- T10: blocked by T2 → blocks T17
- T11: blocked by T2 → blocks T18
- T12: blocked by T3 → blocks T18
- T13: blocked by T4,T7 → blocks T19
- T14: blocked by T5,T8 → blocks T19
- T15: blocked by T6,T9 → blocks T20
- T16: blocked by T7,T8 → blocks T20
- T17: blocked by T9,T10 → blocks T21
- T18: blocked by T11,T12 → blocks T21
- T19: blocked by T13,T14 → blocks F1-F4
- T20: blocked by T15,T16 → blocks F1-F4
- T21: blocked by T17,T18 → blocks F1-F4

### Agent Dispatch Summary
- Wave 1: T1-T6 → quick / unspecified-high (테스트/구조 기초)
- Wave 2: T7-T12 → deep / unspecified-high (계약 정렬)
- Wave 3: T13-T17 → deep (자동화 로직 분해)
- Wave 4: T18-T21 → unspecified-high (통합 회귀)
- Final: F1 oracle, F2 unspecified-high, F3 unspecified-high, F4 deep

---

## TODOs

- [x] 1. Journey 아키텍처/거버넌스 테스트 골격 추가

  **What to do**:
  - `backend/src/test/kotlin/com/manage/crm/journey/architecture/JourneyArchitectureTest.kt` 생성.
  - `backend/src/test/kotlin/com/manage/crm/journey/architecture/JourneyGovernanceTest.kt` 생성.
  - spec 설정은 event/segment 기준으로 journey에 맞는 기본값 정의.

  **Must NOT do**:
  - 규칙을 약화하기 위해 Base 테스트 코드 변경 금지.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: T7, T8, T9
  - **Blocked By**: None

  **References**:
  - `backend/src/test/kotlin/com/manage/crm/event/architecture/EventArchitectureTest.kt`
  - `backend/src/test/kotlin/com/manage/crm/event/architecture/EventGovernanceTest.kt`
  - `backend/src/test/kotlin/com/manage/crm/segment/architecture/SegmentArchitectureTest.kt`
  - `backend/src/test/kotlin/com/manage/crm/segment/architecture/SegmentGovernanceTest.kt`

  **Acceptance Criteria**:
  - [ ] Journey architecture/governance 테스트 파일 2개가 생성됨.
  - [ ] 테스트 클래스가 `ModuleRuleSpec(moduleName = "journey", packageToken = "journey")`를 사용함.

  **QA Scenarios**:
  ```
  Scenario: Journey architecture tests discoverable
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew test --tests "com.manage.crm.journey.architecture.*" --dry-run
      2. 출력에 JourneyArchitectureTest/JourneyGovernanceTest 포함 여부 확인
    Expected Result: 두 테스트 클래스가 실행 대상으로 인식됨
    Evidence: .sisyphus/evidence/task-1-architecture-dryrun.txt

  Scenario: Governance test fails when UC code missing (negative)
    Tool: Bash (gradle)
    Steps:
      1. (테스트 수행 전) journey usecase 일부는 아직 UC 코드 미적용 상태 유지
      2. cd backend && ./gradlew test --tests "com.manage.crm.journey.architecture.JourneyGovernanceTest"
    Expected Result: 초기 상태에서는 실패 가능(의도된 red)
    Evidence: .sisyphus/evidence/task-1-governance-red.txt
  ```

- [x] 2. Journey 테스트 템플릿/테스트 패키지 표준 도입

  **What to do**:
  - `JourneyModuleTestTemplate.kt` 추가 여부 결정 후 event/segment 형식으로 생성.
  - journey 테스트 패키지에 `application/domain/queue/architecture` 디렉토리 구조 표준화.

  **Must NOT do**:
  - 기존 event/segment 테스트 템플릿 수정 금지.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: T10, T11
  - **Blocked By**: None

  **References**:
  - `backend/src/test/kotlin/com/manage/crm/event/EventModuleTestTemplate.kt`
  - `backend/src/test/kotlin/com/manage/crm/segment/SegmentModuleTestTemplate.kt`

  **Acceptance Criteria**:
  - [ ] journey 테스트 경로가 계층별로 분리됨.
  - [ ] 템플릿 도입 시 프로파일/테스트 인프라 등록이 기존 패턴과 동일.

  **QA Scenarios**:
  ```
  Scenario: Test package layout validation
    Tool: Bash
    Steps:
      1. ls backend/src/test/kotlin/com/manage/crm/journey
      2. architecture/application/domain/queue 디렉토리 존재 확인
    Expected Result: 표준 디렉토리 구조 존재
    Evidence: .sisyphus/evidence/task-2-test-layout.txt

  Scenario: Template compile check (negative)
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew testClasses
      2. 템플릿 import/annotation compile 오류 여부 확인
    Expected Result: 컴파일 오류 없음
    Evidence: .sisyphus/evidence/task-2-testclasses.txt
  ```

- [x] 3. Journey DTO 패키지 구조 생성 (`application/dto`)

  **What to do**:
  - `JourneyModels.kt`와 `PutJourneyUseCase.kt` 내부 DTO/IN/OUT 선언을 분리할 dto 파일 구조 설계.
  - `journey/application/dto` 패키지 생성 및 파일 분할 기준 정의.

  **Must NOT do**:
  - 실제 비즈니스 필드 의미 변경 금지.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: T12
  - **Blocked By**: None

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/segment/application/dto/SegmentUseCaseDto.kt`
  - `backend/src/main/kotlin/com/manage/crm/event/application/dto/*.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/application/JourneyModels.kt`

  **Acceptance Criteria**:
  - [ ] journey dto 전용 패키지/파일이 정의됨.
  - [ ] 기존 모델 필드 누락 없이 매핑 계획이 문서화됨.

  **QA Scenarios**:
  ```
  Scenario: DTO package compile path
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew compileKotlin
      2. journey application dto 관련 unresolved 참조 확인
    Expected Result: 컴파일 성공
    Evidence: .sisyphus/evidence/task-3-compile.txt

  Scenario: DTO serialization regression (negative)
    Tool: Bash (gradle)
    Steps:
      1. 기존 controller/usecase 테스트 실행
      2. DTO 이동 후 역직렬화 실패 여부 확인
    Expected Result: 기존 JSON contract 깨지지 않음
    Evidence: .sisyphus/evidence/task-3-contract-regression.txt
  ```

- [x] 4. Automation 분해용 패키지 뼈대 생성

  **What to do**:
  - `journey/application/automation/{condition,segment,execution}` 하위 패키지 생성.
  - 분해 단위(핸들러/정책/헬퍼) 명명 규칙 수립.

  **Must NOT do**:
  - 이 단계에서 로직 동작 변경 금지(파일 이동/분리 준비만).

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: T13
  - **Blocked By**: None

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/journey/application/JourneyAutomationService.kt`
  - `backend/src/main/kotlin/com/manage/crm/event/service/*.kt`

  **Acceptance Criteria**:
  - [ ] automation 하위 패키지가 생성됨.
  - [ ] 분해 책임 매트릭스(어떤 메서드가 어디로 갈지) 정의됨.

  **QA Scenarios**:
  ```
  Scenario: Package skeleton exists
    Tool: Bash
    Steps:
      1. ls backend/src/main/kotlin/com/manage/crm/journey/application/automation
      2. condition/segment/execution 존재 확인
    Expected Result: 패키지 골격 생성
    Evidence: .sisyphus/evidence/task-4-automation-skeleton.txt

  Scenario: No behavior change commit check (negative)
    Tool: Bash (git diff)
    Steps:
      1. git diff 확인
      2. 논리식/분기식 변경 없는지 점검
    Expected Result: 구조 준비 변경만 존재
    Evidence: .sisyphus/evidence/task-4-diff-check.txt
  ```

- [x] 5. Queue 경계 분리 설계 (`application/port` vs `adapter/queue`)

  **What to do**:
  - Journey 외부 노출 계약을 `journey.application.port`로 올리는 설계 작성.
  - `journey.queue` 내부 구현/메시지 타입을 adapter 경계로 축소하는 이동 전략 정의.

  **Must NOT do**:
  - provider 설정(`scheduler.provider`) 동작 변경 금지.

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: T14
  - **Blocked By**: None

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/journey/queue/JourneyTriggerQueuePublisher.kt`
  - `backend/src/main/kotlin/com/manage/crm/event/application/PostEventUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/segment/application/PostSegmentUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/user/application/EnrollUserUseCase.kt`

  **Acceptance Criteria**:
  - [ ] 타 모듈이 참조할 공개 계약 패키지 위치가 명확히 정의됨.
  - [ ] queue 내부 구현 타입 누수 축소 계획이 확정됨.

  **QA Scenarios**:
  ```
  Scenario: Cross-module import reduction baseline
    Tool: Bash (grep)
    Steps:
      1. grep -R "import com.manage.crm.journey.queue" backend/src/main/kotlin/com/manage/crm
      2. baseline 개수 기록
    Expected Result: 현재 결합 baseline 확보
    Evidence: .sisyphus/evidence/task-5-import-baseline.txt

  Scenario: Provider toggle regression guard (negative)
    Tool: Bash (gradle test subset)
    Steps:
      1. queue 관련 테스트 실행
      2. kafka/in-memory conditional bean 생성 실패 여부 확인
    Expected Result: provider 분기 동작 유지
    Evidence: .sisyphus/evidence/task-5-provider-guard.txt
  ```

- [x] 6. Controller 매핑 분리 표준 정의

  **What to do**:
  - JourneyController 인라인 request→usecase 변환을 mapper/extension으로 분리하는 표준 수립.
  - controller는 validation/route/usecase call만 남기도록 기준 정의.

  **Must NOT do**:
  - API endpoint path/HTTP method 변경 금지.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1
  - **Blocks**: T15
  - **Blocked By**: None

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/journey/controller/JourneyController.kt`
  - `backend/src/main/kotlin/com/manage/crm/segment/controller/SegmentController.kt`
  - `backend/src/main/kotlin/com/manage/crm/segment/application/dto/SegmentUseCaseDto.kt`

  **Acceptance Criteria**:
  - [ ] 매핑 로직 분리 위치와 네이밍 규칙 확정.
  - [ ] public API contract 변경 없음.

  **QA Scenarios**:
  ```
  Scenario: Endpoint contract unchanged
    Tool: Bash (curl)
    Steps:
      1. 기존 journey endpoint 목록 호출
      2. 상태코드/응답키 비교
    Expected Result: endpoint contract 동일
    Evidence: .sisyphus/evidence/task-6-endpoint-contract.txt

  Scenario: Invalid request handling unchanged (negative)
    Tool: Bash (curl)
    Steps:
      1. 필수 필드 누락 request 전송
      2. BAD_REQUEST 및 메시지 형식 확인
    Expected Result: validation 에러 동작 유지
    Evidence: .sisyphus/evidence/task-6-validation-negative.txt
  ```

- [x] 7. UseCase annotation/형태 표준화 (`@Component`, `execute(UseCaseIn)`)

  **What to do**:
  - journey usecase 전반을 event/segment 룰에 맞춰 정렬.
  - `pause/resume/archive`는 하나의 execute 입력 모델(예: LifecycleActionIn) 기반으로 통합.

  **Must NOT do**:
  - 라이프사이클 전이 규칙 자체(허용/금지 상태) 변경 금지.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: T16
  - **Blocked By**: T1

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/event/application/BrowseEventsUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/segment/application/PostSegmentUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/application/UpdateJourneyLifecycleStatusUseCase.kt`

  **Acceptance Criteria**:
  - [ ] journey usecase 클래스들이 표준 annotation/execute 시그니처 규칙을 만족.
  - [ ] pause/resume/archive API 계약은 유지되면서 내부 usecase 호출 방식만 표준화.

  **QA Scenarios**:
  ```
  Scenario: Architecture execute-signature pass
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew test --tests "com.manage.crm.journey.architecture.JourneyArchitectureTest"
    Expected Result: execute signature/annotation 규칙 통과
    Evidence: .sisyphus/evidence/task-7-architecture-pass.txt

  Scenario: Lifecycle endpoints still work (negative regression)
    Tool: Bash (curl)
    Steps:
      1. POST /api/v1/journeys/{id}/pause
      2. POST /api/v1/journeys/{id}/resume
      3. POST /api/v1/journeys/{id}/archive
    Expected Result: 기존과 동일한 상태코드/응답 구조 유지
    Evidence: .sisyphus/evidence/task-7-lifecycle-regression.txt
  ```

- [x] 8. Journey UC KDoc + UC 코드 체계 도입

  **What to do**:
  - journey usecase KDoc에 `UC-JOURNEY-001...` 순차 부여.
  - `Input:` `Success:` 섹션 포함 규칙 적용.

  **Must NOT do**:
  - 이미 확정된 event/segment UC 시퀀스/도메인 코드 변경 금지.

  **Recommended Agent Profile**:
  - **Category**: `writing`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: T16
  - **Blocked By**: T1

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/event/application/PostEventUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/segment/application/BrowseSegmentUseCase.kt`
  - `backend/src/test/kotlin/com/manage/crm/architecture/BaseModuleGovernanceTest.kt`

  **Acceptance Criteria**:
  - [ ] 모든 journey usecase가 UC 코드 + Input/Success 섹션 포함.
  - [ ] UC 코드 중복/누락 없이 연속 번호 유지.

  **QA Scenarios**:
  ```
  Scenario: Governance KDoc checks pass
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew test --tests "com.manage.crm.journey.architecture.JourneyGovernanceTest"
    Expected Result: UC code/KDoc 섹션 검증 통과
    Evidence: .sisyphus/evidence/task-8-governance-pass.txt

  Scenario: UC sequence gap check (negative)
    Tool: Bash (gradle)
    Steps:
      1. 임시로 UC 번호 gap 생성 시도(로컬 검증)
      2. Governance test가 실패하는지 확인
    Expected Result: gap 발생 시 테스트 실패
    Evidence: .sisyphus/evidence/task-8-uc-gap-negative.txt
  ```

- [x] 9. Journey DTO 실제 분리 및 import 정리

  **What to do**:
  - `JourneyModels.kt`의 In/Out/Dto를 `application/dto`로 이동.
  - `PutJourneyUseCase.kt` 내부 data class를 dto 파일로 이동.
  - usecase/controller import 경로 정리.

  **Must NOT do**:
  - public response 필드명/필드타입 변경 금지.

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: T17
  - **Blocked By**: T1

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/journey/application/JourneyModels.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/application/PutJourneyUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/segment/application/dto/SegmentUseCaseDto.kt`

  **Acceptance Criteria**:
  - [ ] journey dto가 `application/dto` 패키지로 분리됨.
  - [ ] 기존 API serialization contract 유지.

  **QA Scenarios**:
  ```
  Scenario: DTO move compile & tests
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew compileKotlin
      2. cd backend && ./gradlew test --tests "com.manage.crm.journey.application.*"
    Expected Result: 컴파일/기존 application 테스트 통과
    Evidence: .sisyphus/evidence/task-9-dto-move.txt

  Scenario: JSON contract mismatch detection (negative)
    Tool: Bash (curl)
    Steps:
      1. 주요 journey API 호출
      2. id/name/steps/lifecycleStatus 필드 존재 여부 검사
    Expected Result: 필수 필드 누락 없음
    Evidence: .sisyphus/evidence/task-9-json-contract-negative.txt
  ```

- [x] 10. JourneyModuleTestTemplate 적용 + 테스트 리팩토링 준비

  **What to do**:
  - journey 테스트에서 공통 인프라가 필요한 케이스는 module template 상속 체계 정리.
  - 불필요한 setup 중복 제거.

  **Must NOT do**:
  - 단위테스트를 통합테스트로 무분별하게 승격 금지.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: T17
  - **Blocked By**: T2

  **References**:
  - `backend/src/test/kotlin/com/manage/crm/event/EventModuleTestTemplate.kt`
  - `backend/src/test/kotlin/com/manage/crm/segment/SegmentModuleTestTemplate.kt`
  - `backend/src/test/kotlin/com/manage/crm/journey/application/*.kt`

  **Acceptance Criteria**:
  - [ ] journey 테스트에서 공통 템플릿 적용 지점이 일관됨.
  - [ ] setup/teardown 중복이 감소.

  **QA Scenarios**:
  ```
  Scenario: Journey test compile and run subset
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew test --tests "com.manage.crm.journey.*"
    Expected Result: journey 테스트 스위트 실행 가능
    Evidence: .sisyphus/evidence/task-10-journey-tests.txt

  Scenario: Over-broad integration regression (negative)
    Tool: Bash
    Steps:
      1. 테스트 실행 시간 baseline 비교
      2. 템플릿 적용으로 과도한 느려짐 여부 확인
    Expected Result: 비정상적 시간 급증 없음
    Evidence: .sisyphus/evidence/task-10-runtime-negative.txt
  ```

- [x] 11. Domain 테스트 계층 추가 (엔티티/전이 규칙)

  **What to do**:
  - `journey/domain` 엔티티/전이 규칙 검증 테스트 추가.
  - lifecycle, step order uniqueness, condition expression validation 등 핵심 불변식 우선.

  **Must NOT do**:
  - domain 테스트가 usecase 내부 구현에 과도하게 결합되지 않도록 유지.

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: T18
  - **Blocked By**: T2

  **References**:
  - `backend/src/test/kotlin/com/manage/crm/event/domain/*.kt`
  - `backend/src/test/kotlin/com/manage/crm/segment/domain/*.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/domain/*.kt`

  **Acceptance Criteria**:
  - [ ] journey/domain 테스트 파일 추가.
  - [ ] 핵심 불변식 최소 1 happy + 1 negative 케이스 포함.

  **QA Scenarios**:
  ```
  Scenario: Domain tests pass
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew test --tests "com.manage.crm.journey.domain.*"
    Expected Result: domain test PASS
    Evidence: .sisyphus/evidence/task-11-domain-pass.txt

  Scenario: Invalid invariant fails (negative)
    Tool: Bash (gradle)
    Steps:
      1. invalid condition/duplicate order 케이스 실행
      2. 예외 발생 assertion 확인
    Expected Result: 의도된 예외 assertion 통과
    Evidence: .sisyphus/evidence/task-11-domain-negative.txt
  ```

- [x] 12. Queue 계약/직렬화 테스트 계층 추가

  **What to do**:
  - queue message payload 및 processor/consumer 경계 테스트 추가.
  - EVENT/SEGMENT_CONTEXT 메시지 처리 경로 검증.

  **Must NOT do**:
  - 실제 Kafka infra 의존 E2E를 기본 단위테스트에 강제 금지.

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2
  - **Blocks**: T18
  - **Blocked By**: T3

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/journey/queue/JourneyTriggerQueueMessage.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/queue/JourneyTriggerQueueProcessor.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/queue/JourneyTriggerKafkaConsumer.kt`

  **Acceptance Criteria**:
  - [ ] queue 테스트에서 EVENT/SEGMENT_CONTEXT 경로 모두 검증.
  - [ ] invalid payload/ack failure negative case 포함.

  **QA Scenarios**:
  ```
  Scenario: Queue processor routing pass
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew test --tests "com.manage.crm.journey.queue.*"
    Expected Result: triggerType별 라우팅 테스트 통과
    Evidence: .sisyphus/evidence/task-12-queue-pass.txt

  Scenario: Invalid payload handling (negative)
    Tool: Bash (gradle)
    Steps:
      1. EVENT 메시지에서 event payload null 케이스 실행
      2. 예외/로그 처리 assertion 검증
    Expected Result: graceful failure assertion 통과
    Evidence: .sisyphus/evidence/task-12-queue-negative.txt
  ```

- [ ] 13. Condition 트리거 처리 로직 분해

  **What to do**:
  - `JourneyAutomationService`의 condition 처리(`processConditionTriggeredJourneys`, expression resolution/evaluation)를 별도 handler/policy로 분리.
  - orchestrator는 호출 순서만 유지.

  **Must NOT do**:
  - CONDITION trigger 평가 semantics 변경 금지.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3
  - **Blocks**: T19
  - **Blocked By**: T4, T7

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/journey/application/JourneyAutomationService.kt` (condition-related methods)
  - `backend/src/test/kotlin/com/manage/crm/journey/application/JourneyAutomationServiceTest.kt`

  **Acceptance Criteria**:
  - [ ] condition 처리 코드가 분리 클래스에 위치.
  - [ ] 기존 condition 관련 테스트 시나리오 회귀 없음.

  **QA Scenarios**:
  ```
  Scenario: Condition-trigger happy path
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew test --tests "*JourneyAutomationServiceTest*condition*"
    Expected Result: condition 기반 실행 성공 케이스 통과
    Evidence: .sisyphus/evidence/task-13-condition-pass.txt

  Scenario: Invalid condition expression (negative)
    Tool: Bash (gradle)
    Steps:
      1. invalid expression 케이스 테스트 실행
      2. IllegalArgumentException 처리 확인
    Expected Result: 실패를 정상적으로 감지
    Evidence: .sisyphus/evidence/task-13-condition-negative.txt
  ```

- [ ] 14. Segment 트리거 처리 로직 분해

  **What to do**:
  - segment user/count trigger 처리 메서드를 독립 handler로 분리.
  - state transition 계산과 triggerKey 생성 책임 분리.

  **Must NOT do**:
  - ENTER/EXIT/UPDATE/COUNT_REACHED/COUNT_DROPPED 트리거 규칙 변경 금지.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3
  - **Blocks**: T19
  - **Blocked By**: T5, T8

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/journey/application/JourneyAutomationService.kt` (segment methods)
  - `backend/src/main/kotlin/com/manage/crm/journey/domain/JourneySegmentUserState.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/domain/JourneySegmentCountState.kt`

  **Acceptance Criteria**:
  - [ ] segment 처리 책임이 분리되어 클래스 복잡도 감소.
  - [ ] segment-trigger 회귀 테스트 통과.

  **QA Scenarios**:
  ```
  Scenario: Segment enter/exit/update happy flow
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew test --tests "*JourneyAutomationServiceTest*segment*"
    Expected Result: segment 트리거 시나리오 통과
    Evidence: .sisyphus/evidence/task-14-segment-pass.txt

  Scenario: Threshold crossing false positive 방지 (negative)
    Tool: Bash (gradle)
    Steps:
      1. threshold 미충족/중복 전이 케이스 실행
      2. 실행이 발생하지 않는지 assertion
    Expected Result: 불필요 트리거 방지
    Evidence: .sisyphus/evidence/task-14-threshold-negative.txt
  ```

- [ ] 15. Controller mapper 실제 적용

  **What to do**:
  - JourneyController의 인라인 변환을 mapper/extension으로 이동.
  - controller는 route/validation/usecase 호출만 남김.

  **Must NOT do**:
  - endpoint URL, request/response schema 변경 금지.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3
  - **Blocks**: T20
  - **Blocked By**: T6, T9

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/journey/controller/JourneyController.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/controller/request/*.kt`

  **Acceptance Criteria**:
  - [ ] controller 인라인 변환 로직 대폭 축소.
  - [ ] request→usecaseIn 변환 테스트 가능 구조 확보.

  **QA Scenarios**:
  ```
  Scenario: Journey create/update API happy path
    Tool: Bash (curl)
    Steps:
      1. POST /api/v1/journeys valid payload
      2. PUT /api/v1/journeys/{id} valid payload
      3. 응답의 steps/lifecycleStatus 확인
    Expected Result: 생성/수정 정상 동작
    Evidence: .sisyphus/evidence/task-15-controller-pass.txt

  Scenario: Mapper null/default handling (negative)
    Tool: Bash (curl)
    Steps:
      1. optional 필드 누락 payload 전송
      2. default 처리와 에러 처리 확인
    Expected Result: 기존과 동일한 fallback/에러 동작
    Evidence: .sisyphus/evidence/task-15-controller-negative.txt
  ```

- [ ] 16. Lifecycle UseCase 통합 후 컨트롤러 연결 재정렬

  **What to do**:
  - pause/resume/archive endpoint를 통합 execute 스타일 usecase로 연결.
  - 내부 action enum/DTO 통해 분기.

  **Must NOT do**:
  - endpoint 자체를 통합하거나 폐기하지 말 것(외부 계약 유지).

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3
  - **Blocks**: T20
  - **Blocked By**: T7, T8

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/journey/application/UpdateJourneyLifecycleStatusUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/controller/JourneyController.kt`

  **Acceptance Criteria**:
  - [ ] 내부는 execute(UseCaseIn) 스타일로 통일.
  - [ ] pause/resume/archive API 회귀 없음.

  **QA Scenarios**:
  ```
  Scenario: Lifecycle action matrix happy path
    Tool: Bash (curl)
    Steps:
      1. ACTIVE→PAUSED→ACTIVE→ARCHIVED 순서 호출
      2. 각 응답 lifecycleStatus/version 확인
    Expected Result: 상태 전이 규칙 준수
    Evidence: .sisyphus/evidence/task-16-lifecycle-matrix.txt

  Scenario: Archived journey resume blocked (negative)
    Tool: Bash (curl)
    Steps:
      1. ARCHIVED 상태에서 resume 호출
      2. BAD_REQUEST/예외 메시지 확인
    Expected Result: 금지 전이 차단
    Evidence: .sisyphus/evidence/task-16-archived-negative.txt
  ```

- [ ] 17. DTO 분리 이후 import/패키지 경계 린트 정리

  **What to do**:
  - journey 내부 import 경로를 dto/automation/mapper 구조에 맞춰 정리.
  - 미사용 import, 순환 참조, 잘못된 패키지 참조 제거.

  **Must NOT do**:
  - 문제를 숨기기 위해 suppress 남발 금지.

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3
  - **Blocks**: T21
  - **Blocked By**: T9, T10

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/journey/**/*.kt`
  - `backend/src/test/kotlin/com/manage/crm/journey/**/*.kt`

  **Acceptance Criteria**:
  - [ ] journey 패키지 컴파일/테스트 시 import/package 오류 없음.
  - [ ] 경계 위반 import 감소 확인.

  **QA Scenarios**:
  ```
  Scenario: Full journey compile pass
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew compileKotlin testClasses
    Expected Result: 컴파일/테스트 클래스 생성 성공
    Evidence: .sisyphus/evidence/task-17-compile-pass.txt

  Scenario: Forbidden import detection (negative)
    Tool: Bash (grep)
    Steps:
      1. grep -R "journey.queue" backend/src/main/kotlin/com/manage/crm/{event,segment,user}
      2. 허용되지 않은 direct import 잔존 여부 확인
    Expected Result: direct import 0 또는 계획된 허용 목록만 존재
    Evidence: .sisyphus/evidence/task-17-import-negative.txt
  ```

- [ ] 18. Queue 공개 계약 전환 적용 (타 모듈 참조점 교체)

  **What to do**:
  - event/segment/user에서 journey queue 내부 타입 직접 참조를 포트/공개계약으로 교체.
  - 변환 어댑터는 journey 모듈 내부에 위치.

  **Must NOT do**:
  - 타 모듈 비즈니스 로직 수정 금지(의존점 교체만).

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4
  - **Blocks**: T21
  - **Blocked By**: T11, T12

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/event/application/PostEventUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/segment/application/PostSegmentUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/user/application/EnrollUserUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/queue/*.kt`

  **Acceptance Criteria**:
  - [ ] 타 모듈에서 queue 내부 payload 직접 import 제거.
  - [ ] publish 경로 동작은 기존과 동일.

  **QA Scenarios**:
  ```
  Scenario: Trigger publish integration pass
    Tool: Bash (gradle)
    Steps:
      1. event/segment/user 관련 usecase 테스트 실행
      2. trigger publish 호출 assertion 확인
    Expected Result: 기존 publish 흐름 유지
    Evidence: .sisyphus/evidence/task-18-publish-pass.txt

  Scenario: Message mapping mismatch (negative)
    Tool: Bash (gradle)
    Steps:
      1. payload 필드 누락/타입 불일치 케이스 실행
      2. 안전한 실패 또는 검증 에러 확인
    Expected Result: 잘못된 메시지 매핑 조기 감지
    Evidence: .sisyphus/evidence/task-18-mapping-negative.txt
  ```

- [ ] 19. Automation 분해 통합 테스트 정비

  **What to do**:
  - 분해된 condition/segment/execution 핸들러 통합 흐름 테스트 추가.
  - idempotency, retry, execution history 저장 동작 회귀 검증.

  **Must NOT do**:
  - 실행 성공/실패 판정 기준 변경 금지.

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4
  - **Blocks**: F1-F4
  - **Blocked By**: T13, T14

  **References**:
  - `backend/src/test/kotlin/com/manage/crm/journey/application/JourneyAutomationServiceTest.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/domain/JourneyStepDeduplication.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/domain/JourneyExecutionHistory.kt`

  **Acceptance Criteria**:
  - [ ] automation 분해 후 핵심 회귀 테스트 통과.
  - [ ] 중복 실행 방지/idempotency 시나리오 검증 포함.

  **QA Scenarios**:
  ```
  Scenario: End-to-end automation success path
    Tool: Bash (gradle)
    Steps:
      1. JourneyAutomationServiceTest 전체 실행
      2. dispatch success + history success assertion 확인
    Expected Result: 성공 경로 모두 PASS
    Evidence: .sisyphus/evidence/task-19-automation-pass.txt

  Scenario: Duplicate step dedup handling (negative)
    Tool: Bash (gradle)
    Steps:
      1. dedup 저장 시 DataIntegrityViolation 예외 케이스 실행
      2. SKIPPED_DUPLICATE history 저장 assertion
    Expected Result: 중복 액션 미실행 + 상태 기록
    Evidence: .sisyphus/evidence/task-19-dedup-negative.txt
  ```

- [ ] 20. Controller/API 회귀 및 lifecycle 계약 테스트 확장

  **What to do**:
  - create/update/browse/execution/history/lifecycle endpoint 회귀 세트 보강.
  - request validation 및 error handler 동작 검증.

  **Must NOT do**:
  - API 응답 envelope(`ApiResponse`) 계약 변경 금지.

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4
  - **Blocks**: F1-F4
  - **Blocked By**: T15, T16

  **References**:
  - `backend/src/main/kotlin/com/manage/crm/journey/controller/JourneyController.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/controller/request/*.kt`

  **Acceptance Criteria**:
  - [ ] journey 주요 endpoint 회귀 케이스 확보.
  - [ ] invalid payload/error handler 케이스 포함.

  **QA Scenarios**:
  ```
  Scenario: Journey API suite happy path
    Tool: Bash (curl)
    Steps:
      1. create journey
      2. browse journeys/executions/histories
      3. update + lifecycle actions
    Expected Result: 전체 endpoint 정상 응답
    Evidence: .sisyphus/evidence/task-20-api-pass.txt

  Scenario: Bad request/error response (negative)
    Tool: Bash (curl)
    Steps:
      1. invalid triggerType/empty steps 요청 전송
      2. BAD_REQUEST + fail body 확인
    Expected Result: 일관된 에러 응답
    Evidence: .sisyphus/evidence/task-20-api-negative.txt
  ```

- [ ] 21. Journey 전체 회귀 스위트 통합 및 정리

  **What to do**:
  - architecture/governance/domain/queue/application 테스트를 하나의 회귀 게이트로 묶음.
  - 실패 로그 가독성/실행순서/문서화를 정리.

  **Must NOT do**:
  - flaky 테스트를 임시 disable로 우회 금지.

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: `[]`

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4
  - **Blocks**: F1-F4
  - **Blocked By**: T17, T18

  **References**:
  - `backend/src/test/kotlin/com/manage/crm/journey/**/*.kt`
  - `backend/build.gradle.kts` (test task settings)

  **Acceptance Criteria**:
  - [ ] journey 전체 테스트 스위트가 안정적으로 통과.
  - [ ] 회귀 게이트 명령이 문서화됨.

  **QA Scenarios**:
  ```
  Scenario: Full journey regression gate
    Tool: Bash (gradle)
    Steps:
      1. cd backend && ./gradlew test --tests "com.manage.crm.journey.*"
    Expected Result: journey 전체 PASS
    Evidence: .sisyphus/evidence/task-21-regression-pass.txt

  Scenario: Flaky detection smoke (negative)
    Tool: Bash (gradle)
    Steps:
      1. 동일 테스트 세트를 2회 연속 실행
      2. 결과 일관성 비교
    Expected Result: 비결정적 실패 없음
    Evidence: .sisyphus/evidence/task-21-flaky-negative.txt
  ```

---

## Final Verification Wave (MANDATORY)

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Validate all Must Have / Must NOT Have against code and evidence paths.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | VERDICT`

  **QA Scenarios**:
  ```
  Scenario: Must Have/Must NOT Have checklist execution
    Tool: Bash + Read
    Steps:
      1. Read plan section `Must Have`/`Must NOT Have` and derive checklist items.
      2. For each checklist item, inspect changed files and evidence paths under `.sisyphus/evidence/`.
      3. Produce pass/fail table with file:path evidence references.
    Expected Result: 모든 항목이 binary pass/fail로 판정되고 근거가 남음
    Evidence: .sisyphus/evidence/final-f1-compliance-audit.md

  Scenario: Missing evidence detection (negative)
    Tool: Bash
    Steps:
      1. ls .sisyphus/evidence
      2. 계획된 task evidence 파일 누락 항목 탐지
    Expected Result: 누락 시 REJECT 판정 및 누락 목록 출력
    Evidence: .sisyphus/evidence/final-f1-missing-evidence.txt
  ```

- [ ] F2. **Code Quality Review** — `unspecified-high`
  Run type/lint/test commands and check anti-patterns.
  Output: `Build/Lint/Test status | VERDICT`

  **QA Scenarios**:
  ```
  Scenario: Static quality gate
    Tool: Bash
    Steps:
      1. cd backend && ./gradlew compileKotlin
      2. cd backend && ./gradlew test --tests "com.manage.crm.journey.*"
      3. journey 관련 변경 파일에서 금지 패턴(as any/@ts-ignore/disabled tests/empty catch) 검색
    Expected Result: compile/test PASS + 금지 패턴 0건
    Evidence: .sisyphus/evidence/final-f2-quality-gate.txt

  Scenario: Regression build break detection (negative)
    Tool: Bash
    Steps:
      1. 실패 로그를 파싱하여 첫 실패 지점 식별
      2. 실패 타입(compile/test/style) 분류
    Expected Result: 실패 시 REJECT + 원인 분류 리포트 생성
    Evidence: .sisyphus/evidence/final-f2-failure-classification.txt
  ```

- [ ] F3. **Real Manual QA (Agent-executed)** — `unspecified-high`
  Execute all QA scenarios and integration regression set.
  Output: `Scenarios [N/N] | Integration [N/N] | VERDICT`

  **QA Scenarios**:
  ```
  Scenario: Full planned QA replay
    Tool: Bash (curl) + Bash (gradle)
    Steps:
      1. T1~T21의 QA 시나리오 evidence 파일 존재 여부 확인.
      2. 핵심 API 흐름(create/update/lifecycle/browse execution/history)을 재실행.
      3. automation dedup + segment trigger + condition trigger 핵심 회귀 테스트 재실행.
    Expected Result: 시나리오 재현 결과가 계획 기준과 일치
    Evidence: .sisyphus/evidence/final-f3-qa-replay.md

  Scenario: Cross-task integration failure probe (negative)
    Tool: Bash (curl)
    Steps:
      1. lifecycle 전환 직후 execution/history 조회
      2. segment context 변화 후 trigger 실행 여부 확인
    Expected Result: 연계 경로 실패 시 즉시 REJECT 및 재현 절차 기록
    Evidence: .sisyphus/evidence/final-f3-integration-negative.txt
  ```

- [ ] F4. **Scope Fidelity Check** — `deep`
  Verify diff vs plan (no missing/no creep/no contamination).
  Output: `Tasks [N/N compliant] | VERDICT`

  **QA Scenarios**:
  ```
  Scenario: Diff-to-plan traceability
    Tool: Bash (git) + Read
    Steps:
      1. git diff --name-only로 변경 파일 목록 추출.
      2. 각 파일을 T1~T21 중 어느 task에 매핑되는지 추적.
      3. 매핑되지 않는 파일/작업 누락 항목을 분리 보고.
    Expected Result: 모든 변경이 task와 1:1 매핑되거나 정당화됨
    Evidence: .sisyphus/evidence/final-f4-traceability.md

  Scenario: Scope creep detection (negative)
    Tool: Bash (git)
    Steps:
      1. journey 외 모듈 변경 파일 필터링(event/segment/user 등)
      2. 계획된 의존점 교체 범위를 넘는 변경 탐지
    Expected Result: 범위 초과 변경 발견 시 REJECT
    Evidence: .sisyphus/evidence/final-f4-scope-creep.txt
  ```

---

## Commit Strategy

- Group A (governance + dto/usecase standard): `refactor(journey): standardize governance and usecase contracts`
- Group B (automation decomposition): `refactor(journey): decompose automation workflow handlers`
- Group C (tests and regression hardening): `test(journey): add architecture/governance/domain/queue regression coverage`

---

## Success Criteria

### Verification Commands
```bash
cd backend && ./gradlew test --tests "com.manage.crm.journey.architecture.*"    # PASS
cd backend && ./gradlew test --tests "com.manage.crm.journey.application.*"     # PASS
cd backend && ./gradlew test --tests "com.manage.crm.journey.domain.*"          # PASS
cd backend && ./gradlew test --tests "com.manage.crm.journey.queue.*"           # PASS
```

### Final Checklist
- [ ] Journey 컨벤션이 event/segment 수준의 규칙 및 테스트 게이트를 충족
- [ ] 구조 재정비(경계/책임 분리)가 코드 레벨에서 확인 가능
- [ ] 기존 핵심 동작(이벤트/세그먼트 트리거, 라이프사이클, 실행 이력) 회귀 없음
