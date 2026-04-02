# Modularity Review

**Scope**: Backend modules — `event`, `segment`, `journey`
**Date**: 2026-04-02

## Executive Summary

This CRM platform provides three tightly related core capabilities: event tracking (`event`), user segment targeting (`segment`), and automated journey orchestration (`journey`). All three modules are [core subdomains](https://coupling.dev/posts/dimensions-of-coupling/volatility/) — business-differentiating areas that are actively evolved and carry high [volatility](https://coupling.dev/posts/dimensions-of-coupling/volatility/). The overall coupling posture is **mixed**: the outbound integration contracts between modules are well-designed (each module publishes clean ports for its callers), but Journey's internal automation machinery maintains [model coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) to the Event domain and both Journey and Event duplicate the responsibility of assembling data for Segment evaluation. Two significant imbalances were identified that will cause predictable pain as these core subdomains continue to evolve.

## Coupling Overview Table

| Integration | [Strength](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) | [Distance](https://coupling.dev/posts/dimensions-of-coupling/distance/) | [Volatility](https://coupling.dev/posts/dimensions-of-coupling/volatility/) | [Balanced?](https://coupling.dev/posts/core-concepts/balance/) |
| ----------- | ----------------------------------------------------------------------------------- | ----------------------------------------------------------------------- | --------------------------------------------------------------------------- | -------------------------------------------------------------- |
| `event` → `journey` (`JourneyTriggerPort`) | [Contract](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) | Cross-module (medium) | High — core subdomain | ✅ Yes |
| `segment` → `journey` (`JourneyTriggerPort`) | [Contract](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) | Cross-module (medium) | High — core subdomain | ✅ Yes |
| `event` → `segment` (`SegmentReadPort` surface) | [Contract](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) | Cross-module (medium) | High — core subdomain | ✅ Yes |
| `journey` → `event.domain.Event` (model) | [Model](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) | Cross-module (medium) | High — core subdomain | ❌ No |
| `journey` + `event` → Segment evaluation inputs | [Functional](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) | Cross-module (medium) | High — core subdomain | ❌ No |

---

## Issue: Journey Uses `event.domain.Event` as Internal Carrier Type

**Integration**: `journey.application` → `event.domain`
**Severity**: Significant

### Knowledge Leakage

Journey maintains a proper inbound integration contract at its outer boundary: `JourneyTriggerEventPayload` in `journey.application.port.out` is a clean, journey-owned DTO that callers use to trigger the automation pipeline. However, in `JourneyTriggerQueueProcessor`, this contract is immediately discarded — the payload is translated into `event.domain.Event` (along with `EventProperties` and `EventProperty` from `event.domain.vo`) and passed inward.

From that point, the entire automation layer speaks the Event module's language:

- `JourneyAutomationUseCaseIn` carries `val event: Event?` — Journey's own use-case input DTO references a foreign domain object.
- `ConditionEvaluator.evaluate(conditionExpression, event: Event)` accesses `event.properties.value` directly to evaluate branch and delay conditions.
- `SegmentTriggerHandler.buildSegmentSyntheticEvent(...)` constructs `Event.new(id, name, userId, EventProperties(properties), createdAt)` to represent segment state transitions — synthetic events built from Event's value-object constructors.
- `JourneyAutomationUseCase.resolveStepVariables(...)` iterates `event.properties.value.forEach { put("event.${it.key}", it.value) }`.

The shared knowledge spans: the `Event` class structure, the `EventProperties` and `EventProperty` value-object types, their constructors, and the access pattern `event.properties.value[i].key / .value`. This is [model coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) — Journey shares knowledge of Event's business domain model across a cross-module boundary.

### Complexity Impact

Because all three dimensions are medium or higher — [model-level integration strength](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/), cross-module [distance](https://coupling.dev/posts/dimensions-of-coupling/distance/), and high [volatility](https://coupling.dev/posts/dimensions-of-coupling/volatility/) from core subdomain classification — the [balance rule](https://coupling.dev/posts/core-concepts/balance/) `BALANCE = (STRENGTH XOR DISTANCE) OR NOT VOLATILITY` is not satisfied: neither the strength-distance XOR nor low volatility neutralises the coupling.

A developer modifying the Event domain model must simultaneously assess impact across Journey's queue processor, use-case DTO, condition evaluator, segment trigger handler, and step executor. This spans five distinct files across three layers of Journey's internal architecture, exceeding the 4±1 cognitive units that working memory can hold at once — the textbook definition of [complexity](https://coupling.dev/posts/core-concepts/complexity/).

### Cascading Changes

- **Rename `EventProperties.value`** → fix `ConditionEvaluator`, `JourneyAutomationUseCase.resolveStepVariables`, `SegmentTriggerHandler.processSegmentUserTriggerJourney`
- **Add a required field to `Event` constructor** → fix `JourneyTriggerQueueProcessor` and `SegmentTriggerHandler.buildSegmentSyntheticEvent`
- **Change `EventProperty` to a sealed class** → fix all direct construction sites inside Journey
- **Add a tenant or source field to `Event`** → Journey must decide what value to assign when constructing synthetic segment events — a business decision leaking into an unrelated module

### Recommended Improvement

Introduce a journey-owned event type — e.g. `JourneyTriggerEvent` — inside `journey.application.dto`:

```kotlin
data class JourneyTriggerEvent(
    val id: Long,
    val name: String,
    val userId: Long,
    val properties: Map<String, String>,
    val createdAt: LocalDateTime,
)
```

Translate `JourneyTriggerEventPayload → JourneyTriggerEvent` in `JourneyTriggerQueueProcessor` (replacing the current `→ Event` translation). Update `JourneyAutomationUseCaseIn` to carry `JourneyTriggerEvent?`. Update `ConditionEvaluator`, `SegmentTriggerHandler`, and all step executors to accept `JourneyTriggerEvent` instead of `Event`.

This reduces the [integration strength](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) from model coupling to [contract coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/), which is balanced for the existing cross-module distance. The translation step already exists in `JourneyTriggerQueueProcessor`; this change redirects it to a Journey-owned type instead of a foreign one.

**Trade-off**: One additional type is introduced. In exchange, Journey's automation layer stops importing from `event.domain`, `event.domain.vo`, making the dependency boundary between the two modules enforceable. `journey.package-info.kt` currently declares `event` as an `allowedDependency` — after this change, Journey would only need to depend on `event`'s application port, not its domain model.

---

## Issue: Callers Orchestrate Segment Evaluation Inputs

**Integration**: `journey.application` → `segment`, `event.application` → `segment`
**Severity**: Significant

### Knowledge Leakage

Both `SegmentTriggerHandler` (in journey) and `PostEventUseCase` (in event) implement the same data-gathering protocol before calling `SegmentReadPort`:

1. Fetch all users via `userReadPort.findAll()`
2. Map to `SegmentTargetUserReadModel`
3. Fetch all events for those users via `eventReadPort.findAllByUserIdIn(...)`
4. Map to `SegmentTargetEventReadModel`
5. Call `segmentReadPort.findTargetUserIds(segmentId, users, eventsByUserId)`

This protocol — "segment evaluation requires users AND their event history, assembled by the caller" — is Segment's internal evaluation concern. It has leaked through the port interface design: `SegmentReadPort.findTargetUserIds(segmentId, users, eventsByUserId)` forces every caller to know what data Segment needs and to gather it. This is [functional coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) — the callers share knowledge of Segment's functional requirements.

### Complexity Impact

Each module that calls `SegmentReadPort.findTargetUserIds` must maintain its own data-assembly logic. The assembly code is duplicated verbatim between `SegmentTriggerHandler` and `PostEventUseCase`, creating the conditions for drift: if one caller's assembly is updated and the other's is not, Segment membership evaluates differently across call sites for the same segment and the same data.

A developer evolving Segment's targeting logic (e.g., to support a new condition type) must find and update every caller — not just the Segment module. At present this affects two callers; as the system grows, additional modules may begin evaluating segment membership and inherit the same assembly obligation implicitly.

Because all three modules are [core subdomains](https://coupling.dev/posts/dimensions-of-coupling/volatility/) under active development, the probability of Segment's targeting algorithm evolving is high — making this the definition of an unbalanced, volatile integration.

### Cascading Changes

- **Segment adds a new condition type** (e.g. `purchase.category`) → `SegmentReadPort.findTargetUserIds` needs a new parameter; `SegmentTriggerHandler` and `PostEventUseCase` must fetch and pass the new data
- **Segment optimizes by fetching only events relevant to a segment's conditions** → callers' bulk `findAllByUserIdIn` fetches are no longer aligned with what Segment actually needs; the optimization can't be applied transparently
- **A third module needs to evaluate segment membership** → it must independently discover and implement the same assembly protocol, or risk evaluating membership incorrectly

### Recommended Improvement

Move the data-assembly responsibility into the Segment module. Replace `findTargetUserIds(segmentId, users, eventsByUserId)` on `SegmentReadPort` with a self-contained operation:

```kotlin
interface SegmentReadPort {
    suspend fun resolveTargetUserIds(segmentId: Long): List<Long>
    // existing query operations remain
    suspend fun existsById(segmentId: Long): Boolean
    suspend fun findNameById(segmentId: Long): String?
}
```

`SegmentReadAdapter.resolveTargetUserIds` would use its own `UserReadPort` and `EventReadPort` internally to gather the data it needs, then delegate to `SegmentTargetingService.resolveUserIds`. Callers (`SegmentTriggerHandler`, `PostEventUseCase`) drop their data-assembly code and simply call `segmentReadPort.resolveTargetUserIds(segmentId)`.

**Trade-off**: Segment gains explicit dependencies on `user.application.port.query` and `event.application.port.query`. These dependencies are implicit today (the callers carry them on Segment's behalf); making them explicit inside Segment is more honest about what Segment actually requires. The assembly logic moves from two callers into one canonical location, reducing [functional coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) to [contract coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/). Callers become simpler, and future changes to Segment's evaluation inputs stay inside the Segment module.

---

_This analysis was performed using the [Balanced Coupling](https://coupling.dev) model by [Vlad Khononov](https://vladikk.com)._
