# Forge Platform Core
## Platform Services Specification

**Version**

1.0 (draft — proposal stage, not yet approved for implementation)

## Purpose

This document defines Sprint 2 (Platform Services) before implementation, the same way `12_PLATFORM_KERNEL_SPECIFICATION.md` defined Sprint 1 (Platform Core) before any kernel code existed.

Before this document, "Platform Services" existed only as a six-item name list (`CLAUDE.md` §4, `docs/ARCHITECTURE_STATUS.md`, `docs/10_PLATFORM_CAPABILITY_MAP.md`) — no package structure, no public API, no dependency rules, no sequencing. That is not enough to implement against safely; this document closes that gap at the same level of detail the kernel spec closed it for Core, and no further. Each capability's concrete public API is still a separate, later two-stage proposal (`CLAUDE.md` §29) — this document defines boundaries and sequencing, not final interfaces.

**This document is a proposal.** Per the two-stage discipline, no Platform Services code should be written until this is reviewed and approved.

## 0. Resolving a drift found while drafting this document

`docs/ARCHITECTURE_STATUS.md`'s Platform capability list still includes "Validation" as a Platform-layer item. Sprint 1 already implemented `core.validation` (ADR-021) — but that is not the same "Validation" this list means, and leaving both unexplained would read as a contradiction.

Two distinct things are both called "validation" in this codebase, and ADR-021 already drew this line — this section makes it explicit and citable:

- **`core.validation`** (built): kernel-level invariant/precondition checks — non-null, non-blank, boolean invariants — used inside constructors of `core` types (`TypedId`s, `PlatformError`s). No framework dependency. Throws `NullPointerException`/`IllegalArgumentException`. This is done.
- **Platform-layer validation** (this document, §3 below): boundary/input validation for DTOs and request payloads at the edge of the system (Jakarta Validation annotations, constraint violation → `PlatformError` mapping). This has not started. It is a different concern operating at a different layer, not a duplicate of `core.validation` and not a reason to remove either.

No architecture document is silently rewritten by this clarification — `ARCHITECTURE_STATUS.md`'s Platform list is still accurate; it was only ever a name, and the name was always going to mean boundary validation once elaborated, consistent with kernel spec §8's original split.

## 1. Package Structure

Recommended package root:

`io.forge.platform` (sibling to `io.forge.platform.core`, not nested under it)

Proposed subpackages, one per capability:

- `logging`
- `config`
- `validation` (boundary/Jakarta — see §0)
- `serialization`
- `observability`
- `security`

### Why

`io.forge.platform.platform.*` (nesting Platform under a package literally named `platform`) was the naive alternative and is rejected for the obvious name-stutter — `io.forge.platform` is already the module's own root, standing for "Forge Platform" as a whole, not for the Core layer. `core` is the one specially-named subpackage (the framework-free kernel every layer depends on); every other layer's capabilities are natural siblings of `core`, not children of it. This mirrors how `04_DOMAIN_MODEL.md`'s dependency rule already treats `core` as foundational and everything else as built on top, without needing an intermediate `platform.platform` layer name.

### Alternatives considered

- Nest under `core` (`io.forge.platform.core.logging`, etc.): rejected outright — violates §8's dependency rule (`core` depends on nothing; Platform depends on `core`, meaning Platform code cannot physically live inside the `core` package without inverting that rule).
- One flat `platform` package with no capability subpackages: rejected — six unrelated capabilities (logging, config, validation, serialization, observability, security) sharing one package is exactly the "dumping ground" `05_REPOSITORY_BLUEPRINT.md`/§5 warns against.
- A capability-per-top-level-directory scheme now (`platform/logging/`, `platform/config/` as top-level Maven modules): rejected as premature — `05_REPOSITORY_BLUEPRINT.md` already documents a future multi-module structure but is explicit that the repository "has not yet grown into it." A single module with package-level separation is the correct size for this stage; splitting into Maven modules is a mechanical, low-risk migration to do later if/when independent versioning or build-time isolation is actually needed — not before.

### Trade-offs

Six new packages up front, all currently empty until each capability's own API proposal lands. Accepted for the same reason Core accepted seven-then-eight packages: clear ownership beats a shared dumping ground, and empty-but-named packages cost nothing until populated.

## 2. Dependency Rules (restating and extending §8 for this layer)

- `platform.*` packages may depend on `core`. Never the reverse.
- `platform.*` packages may depend on Spring Boot, Jakarta, and other frameworks — this is the layer `core` is explicitly protected from (`CLAUDE.md` §4: "Platform — cross-cutting engineering capabilities... Core — no framework, no Spring dependency"). This is the first layer in the codebase permitted to import Spring.
- No `platform.*` package should depend on another `platform.*` package unless that dependency is itself justified and stated when that capability's API proposal is written (e.g., `observability` may reasonably depend on `logging`; `logging` should not depend on `observability`). Default assumption until proven otherwise: siblings, not a chain.
- None of the future AI Runtime / Engineering Intelligence / Products layers exist yet; nothing in this document requires anticipating their needs.

## 3. Capabilities (scope only — not yet full API proposals)

Each item below states *what problem it solves* and *what it explicitly excludes*, so implementation doesn't scope-creep into a neighboring capability or a later sprint. Concrete public API (types, method signatures) is a separate proposal per item, written immediately before that item's implementation — same discipline Sprint 1 used per kernel primitive.

### 3.1 Logging

**Solves:** a single, consistent way for any module to emit structured log lines with request/correlation ID, timestamp, and service name (`06_ENGINEERING_STANDARDS.md`: "never `System.out.println`... every log line carries request ID, correlation ID, timestamp, service name").

**Excludes:** log aggregation/shipping infrastructure (ELK, Loki, etc.) — that is deployment-time configuration, not application code, and premature before there is a deployed service to ship logs from.

**Likely shape:** a thin convention/adapter over SLF4J (already Spring Boot's default facade — not a new dependency), not a custom logging framework. Evaluate at proposal time whether any Forge-specific wrapper type is justified at all, or whether "use SLF4J directly, correctly" is the entire deliverable (a real possibility — per §9's own test, "does a real caller need this abstraction, or is it speculative").

### 3.2 Configuration

**Solves:** typed, validated configuration binding (Spring's `@ConfigurationProperties` records) instead of scattered `@Value` string injection, so misconfiguration fails at startup, not at first use.

**Excludes:** a custom configuration-file format or secrets store — Spring's existing property-source mechanism (env vars, `application.yaml`, profiles) is sufficient; introducing a new one would fail the "is a dependency actually necessary" test with no concrete need behind it yet.

### 3.3 Validation (boundary)

**Solves:** Jakarta Validation wiring for DTOs/request bodies once a web layer exists, plus the constraint-violation → `PlatformError` (specifically `DomainError`, since a failed request validation is a business-rule failure, not infrastructure) mapping so API errors follow RFC 9457 (`06_ENGINEERING_STANDARDS.md`) instead of raw Spring validation exceptions leaking through.

**Excludes:** kernel-level checks — already done, see §0. **Blocked on nothing architecturally, but has no concrete caller yet** — there is no REST controller in this repository today. Per §9's own rule ("does a real, current caller need this, or is it speculative"), this item should be the last of the six actually implemented, deferred until a controller exists to validate for, not built ahead of need.

### 3.4 Serialization

**Solves:** consistent JSON (de)serialization conventions for API payloads once a web layer exists — naming strategy, `Optional`/record handling, error-shape consistency.

**Excludes:** a custom serialization engine — Jackson (Spring Boot's default) is the obvious, already-present choice; this capability is conventions and configuration on top of it, not a replacement for it.

**Same caller caveat as §3.3**: no concrete serialization need exists without a web layer. Do not implement ahead of that need.

### 3.5 Observability

**Solves:** metrics and tracing conventions (`14_...`) — this repository already has Micrometer-shaped expectations in its dependency-evaluation culture per `13_CORE_CODING_GUIDELINES.md`'s performance section, and `pom.xml` already has `spring-boot-starter-actuator` — but actuator alone is not "observability," it is the transport; this capability decides what gets measured and how.

**Excludes:** standing up Prometheus/Grafana infrastructure — deployment concern, not application code, and (per the earlier DLMP-style guidance already established in this workspace) premature before there is a running service to observe.

### 3.6 Security Foundations

**Solves:** cross-cutting security primitives usable before a full Identity module exists — secret-handling conventions (never logging secret values, structured handling of `char[]`/`SecretString`-shaped values if a concrete need appears), password-hashing utility wiring (BCrypt, already implied by `06_ENGINEERING_STANDARDS.md`'s general security posture), and secure-header/CORS defaults for whenever a web layer lands.

**Excludes, explicitly, to prevent duplicating a later sprint:** user accounts, authentication, authorization, RBAC, sessions/tokens — all of that is the **Identity** module, sequenced later in the roadmap (`ARCHITECTURE_STATUS.md`: ... AI Runtime → **Identity** → Workspace → ...). Security Foundations provides primitives Identity will later consume; it is not Identity itself. This boundary is the one most likely to blur in practice, so it is stated here explicitly rather than left to be discovered mid-implementation.

**Highest-risk item of the six** — per this session's own security-first mandate, any concrete API proposal for this capability should get explicit review before implementation, not just the standard two-stage discipline.

## 4. Sequencing

**Revision (2026-08-15, ADR-023):** the original recommendation below (Logging first) was written without checking the repository for an actual caller. Checked directly: zero log statements, zero controllers, zero custom configuration properties exist anywhere in `src/main/java`. All six capabilities, not just Validation and Serialization, are currently speculative if implemented today. The sequencing below is preserved for when each capability's caller exists — read it as "this order, once something needs it," not "start now."

Recommended implementation order, evaluated against "does a real, current caller need this":

1. **Logging** — every other capability and every future module benefits immediately; zero dependency on a web layer existing.
2. **Configuration** — same: needed regardless of whether a web layer exists yet, and Logging likely wants typed config (log levels, sampling).
3. **Observability** — builds on Logging/Configuration; actuator dependency already present, so this closes an existing partial gap rather than starting from zero.
4. **Security Foundations** — genuinely needed early (secret-handling discipline should exist before any code that touches secrets), but flagged for extra review per §3.6.
5. **Validation (boundary)** and **6. Serialization** — deliberately last: both are genuinely blocked on "no web layer exists yet" (§3.3, §3.4). Implementing them now would be building ahead of a concrete caller, which this project's own rules reject (`13_CORE_CODING_GUIDELINES.md` §3, `CLAUDE.md` §9).

This sequencing is a recommendation, not a commitment — like Sprint 1's order, it can be revised if implementation evidence changes the picture (e.g., if a web layer decision arrives sooner than expected, promote §3.3/§3.4).

## 5. What this document does not do

It does not implement anything. It does not finalize any capability's public API — each of the six items above still needs its own short API proposal (mirroring how `Result`, `PlatformError`, each `TypedId`, `Clock`, and `Validation` each got proposed before being built) reviewed before code is written for it. It does not commit to Kubernetes, a message broker, a database, or any infrastructure — none of the six capabilities require any of those, and introducing any would fail this project's own anti-over-engineering standard.
