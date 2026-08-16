# PROJECT_STATE.md — Forge AI Platform

**Purpose:** Live, current-state companion to `CLAUDE.md` (which is the stable constitution). This file tracks what's actually built, right now, so it doesn't need to be reconstructed from conversation history.

**Last updated:** Sprint 1 (Platform Core) complete. `ai.provider` implemented (provider-neutral, ADR-024). GitHub blocker narrowed to exactly one step: `gh` CLI installed, needs `gh auth login` (interactive — cannot be done non-interactively). `intelligence.repository` (ADR-025) extracts repository structure + internal package dependencies. New: `intelligence.architecture` (ADR-026) — `CycleDetector` finds circular package dependencies via Tarjan's SCC algorithm, the first real consumer of `intelligence.repository`'s facts. Dogfooded: this repository's own package graph is verified acyclic. 130/130 tests passing.

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

src/main/java/io/forge/platform/intelligence/
└── repository/
    ├── RepositoryScanner.java       (final: scan(Path) -> Result<RepositorySnapshot, PlatformError>;
    │                                  reads pom.xml + walks src/main/java + parses import lines,
    │                                  no AI involved, no compilation required)
    ├── RepositorySnapshot.java      (coordinates, javaVersion, packages, internalDependencies)
    ├── PackageSummary.java          (name, classCount)
    ├── PackageDependency.java       (fromPackage, toPackage — observed internal import facts only;
    │                                  external/JDK/framework imports excluded)
    └── BuildCoordinates.java        (groupId, artifactId, version)

src/main/java/io/forge/platform/intelligence/
└── architecture/
    ├── CycleDetector.java           (final: findCycles(Set<PackageDependency>) -> Set<CyclicPackageGroup>;
    │                                  Tarjan's SCC algorithm, depends on intelligence.repository
    │                                  for PackageDependency — one-directional, ArchUnit-enforced)
    └── CyclicPackageGroup.java      (packages — a finding, not merely an observed fact)
```

130/130 tests passing (includes `ArchitectureTest`, which has no production-code counterpart to list above but enforces §8's `core` dependency rules — now also covering `core`/`intelligence`, `ai`/`intelligence`, and `repository`/`architecture` boundaries, plus cycle-freedom within `intelligence.*`'s own subpackages). `mvn clean verify` and `./mvnw clean verify` both green (Java 25, spotless clean, JaCoCo: 97% instruction / 93% branch coverage — `intelligence.architecture` itself is 100%/100%; the remaining gaps are all pre-existing, documented edge cases in `RepositoryScanner` with no real product risk). `.github/dependabot.yml` added — Maven + GitHub Actions dependency scanning, activates once the repo is pushed to GitHub, no API key required.

**Clean-checkout defect found and fixed:** `mvnw`/`mvnw.cmd`/`.mvn/` were gitignored — a fresh clone had no Maven wrapper at all (verified by actually cloning to a scratch directory, not assumed). This would have broken CI on the first push and every README-documented onboarding step. Fixed: wrapper now tracked, re-verified via a second fresh clone that `./mvnw clean verify` succeeds standalone. CI (`ci.yml`) also given Maven dependency caching (`cache: maven` on `actions/setup-java`).

**`intelligence.repository` (ADR-025):** the first Engineering Intelligence capability. `RepositoryScanner` reads one Maven module's `pom.xml` and `src/main/java` tree into a deterministic `RepositorySnapshot` — build coordinates, Java version, per-package class counts. No AI, no inference, no external dependency beyond the JDK's built-in XML parser. Its own test suite dogfoods it against this repository (asserts the real `pom.xml`'s coordinates, Java 25, and `core.result`'s package/class count) — if the scanner and the repository it's scanning ever disagree, that test fails immediately.

**Cross-session self-review (holistic pass, not scoped to one commit):** audited the full public API surface built so far together for the first time. Two real findings, both documented rather than silently fixed or ignored:
1. **Naming ambiguity**: `core.id.RepositoryId` and `intelligence.repository` name the same English word for two unrelated concepts (a future domain entity identifier vs. concrete filesystem scanning). Explained in `CLAUDE.md` §29 rather than left to confuse a future reader. No code change — linking them now would be speculative (no Workspace/registration concept exists yet to actually issue a `RepositoryId` for a scanned path).
2. **Security note, not yet a vulnerability**: `RepositoryScanner.scan(Path)` has no depth or file-count limit and trusts its `Path` argument completely. Fine today — no caller passes untrusted input; every caller is this codebase's own test suite pointing at a known local directory. Must be revisited (bounds, sandboxing, or explicit trust documentation) before this method is ever reachable from a web endpoint, agent tool, or any caller accepting a path from outside this process.

## Engineering Decisions Recorded This Sprint

- **Value Objects (item 5):** kernel spec §7 makes a shared base type discretionary ("only if it adds real consistency"). The pattern (immutable records, constructor-validated) is already fully demonstrated by `TypedId` and `PlatformError`'s implementations. No concrete domain value object exists yet to build against (no Identity/Workspace module). Building a placeholder example would be a speculative abstraction. **Decision: item considered satisfied by existing pattern; no dedicated code produced.**
- **`DomainEvent.aggregateId()` is `TypedId`, not generic.** A generic `DomainEvent<A extends TypedId>` would give concrete future events a strongly-typed aggregate reference without casting, but no concrete event exists yet to validate that design against, and generifying later is a source-breaking change once real implementers exist. Chose the simpler non-generic contract now; **flagging as a conscious, revisit-if-needed trade-off**, not an oversight.
- **`DomainEvent.version()` is a primitive `long`**, not a dedicated typed value object. Kernel spec §5 only requires "Version" as metadata without further shape; introducing a new `EventVersion` type with no second consumer would be speculative. Revisit if event-sourcing/optimistic-concurrency needs grow.

## Current Blockers

1. **GitHub push**: `gh` CLI is installed but not authenticated. `gh auth status` confirms no logged-in host. Requires an interactive `gh auth login` (browser or device code) — cannot be done non-interactively or fabricated. Everything else (18 clean commits, verified clean-checkout build, no secrets) is ready; this is the only remaining step.
2. **AI provider implementation**: blocked on two decisions that are not mine to make unilaterally (ADR-024) — which provider(s) to integrate, and API credentials. `ai.provider`'s interface is deliberately provider-neutral so this blocker stays contained to one future implementation class.

Everything past `ai.provider`'s interface (`router`, `prompt`, `context`, `tool`, `mcp`, `memory`, `evaluation`, `guardrail`) remains blocked on having at least one real provider integration to validate against (`docs/15_..md` §4). Platform Services (ADR-023) remains deferred, expected to activate once a provider is chosen. Neither blocker affects `intelligence.*` (ADR-025) — that layer needs no AI and no push to keep growing.

## Known, Tracked Issues

- The `platform.*` half of the dependency rule (`io.forge.platform.{logging,config,...}` may depend on `core`, never the reverse) has no ArchUnit rule yet — none of those packages have any classes, so a rule would be untestable/vacuous. Add it alongside the first Platform Services capability's first class.
- `LICENSE` copyright line still reads "Forge AI" rather than "Forge AI Platform" (left untouched deliberately as a legal-document caution).
- Local development requires JDK 25; the default JDK on this machine was 21. Installed Temurin 25 to `~/jdks` (user-local, no sudo) to build — not yet documented in README/CONTRIBUTING as a prerequisite.
