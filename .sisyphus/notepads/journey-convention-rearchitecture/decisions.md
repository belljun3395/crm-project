# Decisions

- Journey architecture test files were already created with correct spec alignment
- Architecture test specs kept consistent across modules: Event, Segment, Journey
- Governance test minimal spec pattern maintained
- Successfully verified via gradle dry-run

## T2: Journey Test Template Standard

- JourneyModuleTestTemplate.kt follows Event/Segment exact annotation/profile pattern
- Package structure mirrors Event/Segment: application/, architecture/, domain/, service/
- domain/ subpackage created to complete standardization
- Template inheritance from BehaviorSpec with SpringBootTest integration ensured consistency

## T3: Journey DTO Package Split

- Extracted DTOs to dedicated dto/ subdirectory following Event/Segment pattern
- Split strategy: JourneyModelsDto.kt for all enums+core DTOs, PutJourneyUseCaseDto.kt for Put-specific DTOs (following Event pattern)
- Backward compatibility: JourneyModels.kt now re-exports via typealiases to minimize breaking changes for existing code
- Import updates: Controller and PutJourneyUseCase updated to import from dto package directly
- All extract-only changes, no refactoring of business logic in this task
- DTO structure now ready for T9 (comprehensive serialization standardization)

