# PROJECT_STATE.md — Forge AI Platform

**Purpose:** Live, current-state companion to `CLAUDE.md` (which is the stable constitution). This file tracks what's actually built, right now, so it doesn't need to be reconstructed from conversation history.

**Last updated:** Sprint 1 (Platform Core) complete. Sprint 2 (Platform Services) fork (ADR-023) resolved: proceeding to Sprint 3 (AI Runtime) instead, per architecture (`Core → Platform → AI Runtime`) — AI Runtime calling a real provider will give Platform Services its first genuine callers. `docs/15_AI_RUNTIME_SPECIFICATION.md` + ADR-024 drafted; `io.forge.platform.ai.provider` (the one piece buildable without a provider decision or API key) implemented: `AiProvider`, `AiPrompt`, `AiCompletion`. 76/76 tests passing.

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

src/main/java/io/forge/platform/ai/
└── provider/
    ├── AiProvider.java              (@FunctionalInterface: complete(AiPrompt) -> Result<AiCompletion, PlatformError>;
    │                                  static fixed(...)/failing(...) test doubles — no vendor SDK, no API key)
    ├── AiPrompt.java
    └── AiCompletion.java
```

76/76 tests passing. `mvn clean verify` and `./mvnw clean verify` both green (Java 25, spotless clean, JaCoCo: 95% instruction / 100% branch coverage).

## Engineering Decisions Recorded This Sprint

- **Value Objects (item 5):** kernel spec §7 makes a shared base type discretionary ("only if it adds real consistency"). The pattern (immutable records, constructor-validated) is already fully demonstrated by `TypedId` and `PlatformError`'s implementations. No concrete domain value object exists yet to build against (no Identity/Workspace module). Building a placeholder example would be a speculative abstraction. **Decision: item considered satisfied by existing pattern; no dedicated code produced.**
- **`DomainEvent.aggregateId()` is `TypedId`, not generic.** A generic `DomainEvent<A extends TypedId>` would give concrete future events a strongly-typed aggregate reference without casting, but no concrete event exists yet to validate that design against, and generifying later is a source-breaking change once real implementers exist. Chose the simpler non-generic contract now; **flagging as a conscious, revisit-if-needed trade-off**, not an oversight.
- **`DomainEvent.version()` is a primitive `long`**, not a dedicated typed value object. Kernel spec §5 only requires "Version" as metadata without further shape; introducing a new `EventVersion` type with no second consumer would be speculative. Revisit if event-sourcing/optimistic-concurrency needs grow.

## Current Blocker

Connecting a real `AiProvider` implementation is blocked on two decisions that are not mine to make unilaterally (ADR-024):

1. **Which AI provider(s) to integrate** (Anthropic, OpenAI, both behind the abstraction, a local model) — cost, licensing, and product-direction implications.
2. **API credentials** for that provider — a hard stop per this project's own rules; cannot be obtained or guessed.

Everything past `ai.provider`'s interface (`router`, `prompt`, `context`, `tool`, `mcp`, `memory`, `evaluation`, `guardrail`) is additionally blocked on having at least one real provider integration to validate against, or a concrete product feature that doesn't exist yet (`docs/15_..md` §4's per-capability table) — not sequenced for implementation until then.

Platform Services (ADR-023) remains deferred, expected to pick up its first real capability (most likely Configuration, for provider API keys/model settings) once a provider is chosen.

## Known, Tracked Issues

- No automated architecture-boundary test yet enforcing `core`/`platform` dependency rules (§13, §40).
- `Result.java`, `FixedClock.java`, and `InternalUuidGenerator.java` still use inline `Objects.requireNonNull` instead of the new `Validation` helper — deliberately out of Validation's approved retrofit scope (`TypedId`/`PlatformError` only); candidate for a small future cleanup.
- `LICENSE` copyright line still reads "Forge AI" rather than "Forge AI Platform" (left untouched deliberately as a legal-document caution).
- Local development requires JDK 25; the default JDK on this machine was 21. Installed Temurin 25 to `~/jdks` (user-local, no sudo) to build — not yet documented in README/CONTRIBUTING as a prerequisite.
- No GitHub remote configured yet — repository exists only locally pending manual repo creation.
