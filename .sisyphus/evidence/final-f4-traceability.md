# Final F4 Traceability Matrix

Date: 2026-04-01 (Asia/Seoul)
Plan: `.sisyphus/plans/journey-convention-rearchitecture.md`

## Traceability (Final Wave Focus)

### T16/T17 (usecase & DTO convention continuation)
- Evidence of DTO/package convention propagation in journey usecases/tests:
  - `backend/src/main/kotlin/com/manage/crm/journey/application/BrowseJourneyUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/application/BrowseJourneyExecutionUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/application/BrowseJourneyExecutionHistoryUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/application/PutJourneyUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/application/PostJourneyUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/application/UpdateJourneyLifecycleStatusUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/application/dto/*`

### T18 (queue boundary decoupling)
- Journey trigger port/payload introduced and used:
  - `backend/src/main/kotlin/com/manage/crm/journey/application/port/out/JourneyTriggerPort.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/application/port/out/JourneyTriggerPayloads.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/queue/JourneyTriggerQueuePublisher.kt`
- Cross-module coupling points updated to port:
  - `backend/src/main/kotlin/com/manage/crm/event/application/PostEventUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/segment/application/PostSegmentUseCase.kt`
  - `backend/src/main/kotlin/com/manage/crm/user/application/EnrollUserUseCase.kt`

### T19 (automation decomposition + tests)
- Decomposition units:
  - `backend/src/main/kotlin/com/manage/crm/journey/application/automation/condition/ConditionExpressionResolver.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/application/automation/condition/ConditionTriggerHandler.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/application/automation/segment/SegmentTriggerHandler.kt`
- Tests:
  - `backend/src/test/kotlin/com/manage/crm/journey/application/automation/condition/ConditionExpressionResolverTest.kt`
  - `backend/src/test/kotlin/com/manage/crm/journey/application/automation/condition/ConditionTriggerHandlerTest.kt`
  - `backend/src/test/kotlin/com/manage/crm/journey/application/automation/segment/SegmentTriggerHandlerTest.kt`

### T20 (controller mapping integration)
- Mapper extraction + integration test:
  - `backend/src/main/kotlin/com/manage/crm/journey/controller/mapper/JourneyRequestMapper.kt`
  - `backend/src/main/kotlin/com/manage/crm/journey/controller/JourneyController.kt`
  - `backend/src/test/kotlin/com/manage/crm/journey/controller/JourneyControllerIntegrationTest.kt`

### T21 (cross-module regression coverage)
- Regression tests updated for new trigger-port boundary:
  - `backend/src/test/kotlin/com/manage/crm/event/application/PostEventUseCaseTest.kt`
  - `backend/src/test/kotlin/com/manage/crm/segment/application/PostSegmentUseCaseTest.kt`
  - `backend/src/test/kotlin/com/manage/crm/user/application/EnrollUserUseCaseTest.kt`

## Final Gate Linkage

- F1: `final-f1-compliance-audit.md`
- F2: `final-f2-quality-gate.txt`
- F3: `final-f3-qa-replay.md`
- F4: this file

## Status

- Final wave traceability: **COMPLETE**
