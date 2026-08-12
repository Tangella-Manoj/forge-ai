# PROJECT_STATE.md — Forge AI Platform

**Purpose:** Live, current-state companion to `CLAUDE.md` (which is the stable constitution). This file tracks what's actually built, right now, so it doesn't need to be reconstructed from conversation history.

**Last updated:** Sprint 1, Clock + Domain Events complete; Validation blocked pending an architecture decision (see below).

---

## Sprint Status

Sprint 0 (Foundation): ✅ Done.

Sprint 1 (Platform Core) — in progress:

| # | Item | Status |
|---|---|---|
| 1 | `Result<T,E>` | ✅ Implemented, tested, merged |
| 2 | `PlatformError` (`DomainError`/`InfrastructureError`) | ✅ Implemented, tested, merged |
| 3 | Typed IDs (`WorkspaceId`, `RepositoryId`, `DecisionId`, `AnalysisId`, `EventId`) | ✅ Implemented, tested, merged |
| 4 | Clock | ✅ Implemented, tested, merged |
| 5 | Value Objects | ✅ Resolved by engineering decision — no new artifact required (see below); no code produced |
| 6 | Domain Events | ✅ Implemented, tested, merged |
| 7 | Validation | 🟡 Unblocked (ADR-021: `core.validation` package added to kernel spec) — API proposal not yet started |
| 8 | Core Tests (broader hardening pass) | Not started — comes after Validation |

## What Exists Right Now

```
src/main/java/io/forge/platform/
├── ForgePlatformApplication.java
└── core/
    ├── error/
    │   ├── PlatformError.java       (sealed interface)
    │   ├── DomainError.java
    │   └── InfrastructureError.java
    ├── event/
    │   └── DomainEvent.java         (interface: eventId, occurredAt, aggregateId, version)
    ├── id/
    │   ├── TypedId.java             (interface)
    │   ├── InternalUuidGenerator.java (package-private UUIDv7 strategy)
    │   ├── WorkspaceId.java
    │   ├── RepositoryId.java
    │   ├── DecisionId.java
    │   ├── AnalysisId.java
    │   └── EventId.java
    ├── result/
    │   └── Result.java              (sealed interface: Success/Failure)
    └── time/
        ├── Clock.java                (@FunctionalInterface)
        ├── SystemClock.java          (package-private)
        └── FixedClock.java           (package-private)
```

46/46 tests passing. `mvn clean verify` and `./mvnw clean verify` both green (Java 25, spotless clean, JaCoCo reporting).

## Engineering Decisions Recorded This Sprint

- **Value Objects (item 5):** kernel spec §7 makes a shared base type discretionary ("only if it adds real consistency"). The pattern (immutable records, constructor-validated) is already fully demonstrated by `TypedId` and `PlatformError`'s implementations. No concrete domain value object exists yet to build against (no Identity/Workspace module). Building a placeholder example would be a speculative abstraction. **Decision: item considered satisfied by existing pattern; no dedicated code produced.**
- **`DomainEvent.aggregateId()` is `TypedId`, not generic.** A generic `DomainEvent<A extends TypedId>` would give concrete future events a strongly-typed aggregate reference without casting, but no concrete event exists yet to validate that design against, and generifying later is a source-breaking change once real implementers exist. Chose the simpler non-generic contract now; **flagging as a conscious, revisit-if-needed trade-off**, not an oversight.
- **`DomainEvent.version()` is a primitive `long`**, not a dedicated typed value object. Kernel spec §5 only requires "Version" as metadata without further shape; introducing a new `EventVersion` type with no second consumer would be speculative. Revisit if event-sourcing/optimistic-concurrency needs grow.

## Current Blocker

None. The kernel-spec gap (§1 subpackage list vs. §8 Validation Strategy) is resolved: `core.validation` added as an eighth kernel subpackage (ADR-021). Next action is the item-7 API proposal (package structure, public types, factory methods, justification — per the two-stage discipline in `CLAUDE.md` §29) — not yet started, and implementation should not begin until that proposal is reviewed and approved.

## Known, Tracked, Pre-Existing Issues (unrelated to this sprint's work, not touched)

- `src/main/java/ai/forge/ForgeAiApplication.java` — staged-then-deleted leftover from the pre-`io.forge.platform` package naming attempt. Still present in the git index.
- No automated architecture-boundary test yet enforcing `core`/`platform` dependency rules.
- README's "Key documents" list is stale relative to `docs/INDEX.md`.
- `LICENSE` copyright line still reads "Forge AI" rather than "Forge AI Platform" (left untouched deliberately as a legal-document caution).
