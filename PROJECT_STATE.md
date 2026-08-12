# PROJECT_STATE.md — Forge AI Platform

**Purpose:** Live, current-state companion to `CLAUDE.md` (which is the stable constitution). This file tracks what's actually built, right now, so it doesn't need to be reconstructed from conversation history.

**Last updated:** Sprint 1 (Platform Core) complete. Sprint 2 (Platform Services) specification drafted (`docs/14_PLATFORM_SERVICES_SPECIFICATION.md`, ADR-022) — proposal stage, not yet approved for implementation. No Sprint 2 code written.

---

## Sprint Status

Sprint 0 (Foundation): ✅ Done.

Sprint 1 (Platform Core): ✅ Done.

| # | Item | Status |
|---|---|---|
| 1 | `Result<T,E>` | ✅ Implemented, tested, merged |
| 2 | `PlatformError` (`DomainError`/`InfrastructureError`) | ✅ Implemented, tested, merged |
| 3 | Typed IDs (`WorkspaceId`, `RepositoryId`, `DecisionId`, `AnalysisId`, `EventId`) | ✅ Implemented, tested, merged |
| 4 | Clock | ✅ Implemented, tested, merged |
| 5 | Value Objects | ✅ Resolved by engineering decision — no new artifact required (see below); no code produced |
| 6 | Domain Events | ✅ Implemented, tested, merged |
| 7 | Validation | ✅ Implemented, tested, merged (ADR-021) |
| 8 | Core Tests (broader hardening pass) | ✅ Closed the one real gap found: `InternalUuidGenerator` had zero direct tests despite gatekeeping every typed ID's validity — added boundary tests (version-vs-variant isolated) and a concurrency test (10k virtual-thread-concurrent generations, zero collisions) |

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
    ├── time/
    │   ├── Clock.java                (@FunctionalInterface)
    │   ├── SystemClock.java          (package-private)
    │   └── FixedClock.java           (package-private)
    └── validation/
        └── Validation.java          (final utility: requireNonNull/requireTrue/requireNonBlank)
```

64/64 tests passing. `mvn clean verify` and `./mvnw clean verify` both green (Java 25, spotless clean, JaCoCo: 95%+ instruction coverage on `core`).

## Engineering Decisions Recorded This Sprint

- **Value Objects (item 5):** kernel spec §7 makes a shared base type discretionary ("only if it adds real consistency"). The pattern (immutable records, constructor-validated) is already fully demonstrated by `TypedId` and `PlatformError`'s implementations. No concrete domain value object exists yet to build against (no Identity/Workspace module). Building a placeholder example would be a speculative abstraction. **Decision: item considered satisfied by existing pattern; no dedicated code produced.**
- **`DomainEvent.aggregateId()` is `TypedId`, not generic.** A generic `DomainEvent<A extends TypedId>` would give concrete future events a strongly-typed aggregate reference without casting, but no concrete event exists yet to validate that design against, and generifying later is a source-breaking change once real implementers exist. Chose the simpler non-generic contract now; **flagging as a conscious, revisit-if-needed trade-off**, not an oversight.
- **`DomainEvent.version()` is a primitive `long`**, not a dedicated typed value object. Kernel spec §5 only requires "Version" as metadata without further shape; introducing a new `EventVersion` type with no second consumer would be speculative. Revisit if event-sourcing/optimistic-concurrency needs grow.

## Current Blocker

Sprint 2 implementation is blocked on review of `docs/14_PLATFORM_SERVICES_SPECIFICATION.md` (drafted this session, ADR-022) — specifically the package structure (`io.forge.platform.{logging,config,validation,serialization,observability,security}`) and the recommended sequencing (Logging → Configuration → Observability → Security Foundations → Validation/Serialization, the last two deliberately deferred until a web layer exists to validate/serialize for). Each of the six capabilities still needs its own short API proposal before its implementation, same two-stage discipline as every Sprint 1 kernel primitive. Security Foundations (§3.6 of the spec) is flagged for extra review given its risk profile, even after the overall spec is approved.

## Known, Tracked Issues

- No automated architecture-boundary test yet enforcing `core`/`platform` dependency rules (§13, §40).
- `Result.java`, `FixedClock.java`, and `InternalUuidGenerator.java` still use inline `Objects.requireNonNull` instead of the new `Validation` helper — deliberately out of Validation's approved retrofit scope (`TypedId`/`PlatformError` only); candidate for a small future cleanup.
- `LICENSE` copyright line still reads "Forge AI" rather than "Forge AI Platform" (left untouched deliberately as a legal-document caution).
- Local development requires JDK 25; the default JDK on this machine was 21. Installed Temurin 25 to `~/jdks` (user-local, no sudo) to build — not yet documented in README/CONTRIBUTING as a prerequisite.
- No GitHub remote configured yet — repository exists only locally pending manual repo creation.
