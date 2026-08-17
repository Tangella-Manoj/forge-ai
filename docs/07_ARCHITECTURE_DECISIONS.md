# Forge AI Platform
## Architecture Decision Records

**Version**

1.0

## Purpose

Every important engineering decision must answer:

Why?
Why not the alternatives?
What trade-offs did we accept?

Future contributors (including AI) should understand why a decision exists before changing it.

## ADR-001

### Project Type

#### Decision

Forge AI will be built as a Modular Monolith.

#### Why

Faster development.
Easier debugging.
Simpler deployment.
Lower operational complexity.
Perfect for one primary maintainer.
Easier AI-assisted development.

#### Why NOT Microservices

Microservices introduce:

distributed transactions,
service discovery,
deployment complexity,
observability complexity,
higher operational cost.

None of those are justified for Version 1.

#### Migration Strategy

Every module must be designed so it can become an independent service later without changing business logic.

## ADR-002

### Programming Language

#### Decision

Java 25 LTS

_(Updated from the original Java 21 LTS decision as Java 25 became the current LTS release. The rationale below is unchanged; only the version number was superseded.)_

#### Why

Enterprise adoption.
Excellent JVM ecosystem.
Spring Boot maturity.
Virtual Threads.
Strong tooling.
High performance.
Long-term support.

#### Alternatives Considered

Python

Advantages

Excellent AI ecosystem.

Rejected because

Java better aligns with the backend engineering focus of the platform.

Go

Advantages

Simplicity.
Concurrency.

Rejected because

Smaller enterprise ecosystem for this type of application.

Rust

Advantages

Performance.
Safety.

Rejected because

Increased development complexity for a solo project.

## ADR-003

### Framework

#### Decision

Spring Boot

#### Why

Mature ecosystem.
Excellent testing support.
Security.
Observability.
Dependency Injection.
Enterprise adoption.

## ADR-004

### Architecture Style

#### Decision

Clean Architecture

DDD

Vertical Slice Architecture

#### Why

Business first.

Framework second.

## ADR-005

### Database

#### Decision

PostgreSQL

#### Why

ACID.
JSON support.
Full-text search.
Mature indexing.
Rich SQL capabilities.

#### Not because

"It is popular."

## ADR-006

### Cache

#### Decision

Redis

#### Purpose

Caching.

Distributed locks.

Rate limiting.

Temporary state.

## ADR-007

### Messaging

#### Decision

Kafka

#### Purpose

Event-driven communication.

Scalability.

Replay capability.

Auditability.

#### Why not RabbitMQ?

RabbitMQ is excellent for task queues and request/reply messaging.

Kafka is chosen here because the platform is expected to benefit from durable event streams and event replay as it grows.

This doesn't mean RabbitMQ is "bad"; it means Kafka better matches the primary architectural direction of this project.

## ADR-008

### Authentication

#### Decision

OAuth2

OIDC

JWT

RBAC

Never invent custom authentication.

## ADR-009

### Observability

#### Decision

OpenTelemetry

Prometheus

Grafana

Structured Logs

Observability is a first-class feature.

Not an afterthought.

## ADR-010

### AI Layer

#### Decision

AI providers

must be abstracted.

Never tightly couple the platform to one vendor.

Example

Today

OpenAI

Tomorrow

Anthropic

Later

Local models

No business logic should change.

## ADR-011

### API

#### Decision

REST

for external APIs.

Internal modules

communicate

through interfaces

and domain events.

## ADR-012

### Testing

#### Decision

Mandatory

Unit Tests

Integration Tests

Contract Tests

Architecture Tests

Architecture tests

are especially important.

We want automated checks that prevent accidental violations of module boundaries.

## ADR-013

### AI Philosophy

#### Decision

AI

does NOT own

business logic.

AI

assists

business logic.

This is permanent.

## ADR-014

### Security

#### Decision

Secure by default.

Every feature

must assume

hostile input.

## ADR-015

### Performance

#### Decision

Measure.

Never guess.

Optimization

must always be based on

profiling,

metrics,

or benchmarks.

## ADR-016

### Documentation

#### Decision

Documentation

is

part of the product.

Not

an optional task.

## ADR-017

### Open Source

#### Decision

Everything

must be understandable

by contributors

without

private knowledge.

## ADR-018

### AI Coding Policy

#### Decision

GitHub Copilot

Claude Code

other AI tools

are allowed.

However,

generated code

must always be:

reviewed,
tested,
and understood

before being merged.

This keeps the project maintainable regardless of which coding assistant produced the code.

## ADR-019

### Long-Term Direction

#### Decision

Forge AI is designed as an Engineering Intelligence Platform, not a general-purpose AI platform.

Every new feature should strengthen this mission.

## ADR-020

### Final Principle

#### Decision

Before adding a feature,

ask:

Does this make Forge AI better at helping engineering teams understand, build, operate, or improve software?

If the answer is no, reconsider whether it belongs in the project.

## ADR-021

### Kernel Validation Package

#### Decision

Add `core.validation` as an eighth kernel subpackage, alongside `id`, `result`, `error`, `event`, `time`, `valueobject`, `version` (kernel spec §1).

#### Why

Kernel spec §8 (Validation Strategy) already describes concrete, real guidance for kernel-level validation — shared invariant checks for IDs, value objects, and platform primitives — but no package was ever listed for it. Every other numbered section in the kernel spec maps to exactly one listed subpackage; Validation was the sole exception. This was a documentation gap, not a deliberate omission.

#### Alternatives considered

- Fold kernel validation helpers into `valueobject`, per §8's own wording ("IDs, value objects, and platform primitives"). Rejected: kernel validation applies across `id`, `error`, and future `valueobject` types, not only value objects — housing it inside `valueobject` would misrepresent its scope and create an awkward dependency shape (`id` and `error` reaching into `valueobject` for shared validation helpers).
- Leave unresolved and continue ad hoc, per-type validation inside each compact constructor indefinitely. Rejected: this is what's already happening (§8 itself calls out that this is "ad hoc" today) and was the reason Sprint 1 flagged it as a blocker rather than silently continuing.

#### Trade-offs

One more top-level kernel package to maintain. Accepted — it is a small, focused package (shared validation helpers only, no framework dependency), consistent with the kernel's existing package-per-concern structure.

#### Long-term impact

Kernel spec §1's subpackage list and §8's Validation Strategy section now agree. Track A (Platform Core) Sprint 1 item 7 (Validation) is unblocked.

## ADR-022

### Platform Services Package Structure

#### Decision

Platform Services (Sprint 2) live under `io.forge.platform.*` as siblings of `core` — `logging`, `config`, `validation`, `serialization`, `observability`, `security` — not nested inside `core`, and not as a `platform.platform.*` intermediate layer. Full rationale, scope, and sequencing: `docs/14_PLATFORM_SERVICES_SPECIFICATION.md`.

This ADR also resolves an inconsistency in `ARCHITECTURE_STATUS.md`: its Platform capability list includes "Validation," which could be misread as duplicating `core.validation` (ADR-021). They are different concerns at different layers — kernel-level invariant checks (`core.validation`, done) versus boundary/DTO validation (`platform.validation`, not started) — matching the split kernel spec §8 already drew. Neither document needed correcting; the ambiguity only existed until it was written down explicitly.

#### Why

Sprint 1 had a full specification (`docs/12_...md`) before any kernel code existed. Sprint 2 had only a six-item name list. Implementing against a name list risks each capability landing in an inconsistent location or duplicating another capability's concern (the Validation ambiguity above is exactly that risk, caught before it became real code).

#### Alternatives considered

- Nest Platform under `core` — rejected: inverts the dependency rule that `core` depends on nothing (§8).
- One flat `platform` package for all six capabilities — rejected: recreates the "dumping ground" anti-pattern §5 already warns against.
- Multi-module Maven split now — rejected as premature; `05_REPOSITORY_BLUEPRINT.md` already documents this as a future structure the repository hasn't grown into yet.

#### Trade-offs

Six named-but-empty packages exist before any of them has code. Accepted — matches how Core's kernel spec pre-declared its subpackages before implementation.

#### Long-term impact

Sprint 2 can proceed capability-by-capability (each still needing its own short API proposal, per the two-stage discipline) without re-litigating package placement per item.

## ADR-023

### Defer Sprint 2 Implementation Until a Concrete Caller Exists

#### Decision

Do not implement any of the six Platform Services capabilities yet, including Logging (which ADR-022 recommended starting first). Defer all of Sprint 2 until at least one concrete caller exists for it — either the first domain module (Identity, per the roadmap) or a deliberate decision to build a minimal web layer first.

#### Why

ADR-022 recommended Logging as the first capability, reasoning it had "zero dependency on a web layer existing." That reasoning was incomplete. Checked directly against the repository: zero log statements exist anywhere in `src/main/java`, zero `@RestController`s, zero custom `@Value`/`@ConfigurationProperties` usage. Spring Boot's default Logback output already provides timestamp/level/thread/logger with no configuration. There is no request-processing flow for a correlation ID (§3.1 of the Platform Services spec) to correlate. Writing logging configuration now would have no log statement to validate it against — the same "no concrete caller" problem ADR-022 already correctly identified for Validation and Serialization turns out to apply to all six capabilities, not just two, once checked against actual repository evidence instead of assumed.

#### Alternatives considered

- Implement Logging anyway, as scaffolding for whenever a caller arrives: rejected — this is precisely the "framework leakage" and "speculative infrastructure" this project's own standards (`13_CORE_CODING_GUIDELINES.md` §3, `CLAUDE.md` §9) reject, and there would be no way to test it meaningfully (no log output exists to assert against).
- Build a minimal web layer now specifically to unblock Platform Services: rejected as a decision to make deliberately, not as a side effect of "Logging needs something to log about." Building a controller merely to justify already-planned infrastructure work is backwards — the controller should exist because a feature needs it.
- Proceed to Sprint 3 (AI Runtime) instead, skipping Sprint 2 entirely: not rejected, but not decided here either — this is a genuine fork between two materially different directions, addressed as an open decision below rather than resolved unilaterally.

#### Trade-offs

Sprint 2 stays fully speculative (spec written, ADR-022 package structure still valid, zero code) for longer than originally planned. Accepted — the alternative (building unvalidatable infrastructure) is worse.

#### Long-term impact

`docs/14_PLATFORM_SERVICES_SPECIFICATION.md` §4 (Sequencing) is corrected by a dated revision note, not silently rewritten. The package structure and per-capability scope (§1–§3 of that document) remain valid and unaffected — only the "build now" sequencing claim was wrong.

## ADR-024

### AI Runtime Package Structure and `provider` as the First Buildable Piece

#### Decision

Proceed to Sprint 3 (AI Runtime) next, resolving the ADR-023 fork: not by building Platform Services speculatively, and not by building a throwaway web layer, but by moving to the layer the architecture already places next (`Core → Platform → AI Runtime`), which genuinely needs Configuration, Logging, Observability, and Security Foundations the moment it calls a real provider — giving Platform Services real callers as a consequence of real need, not as the goal.

AI Runtime lives under `io.forge.platform.ai.*` (sibling of `core`, not nested under `core` or the Platform packages), with one subpackage per capability from the existing name list: `provider`, `router`, `prompt`, `context`, `tool`, `mcp`, `memory`, `evaluation`, `guardrail`. Full rationale: `docs/15_AI_RUNTIME_SPECIFICATION.md`.

Of these nine, only `provider`'s **interface** is implemented now — `AiProvider` (vendor-agnostic `complete(AiPrompt) -> Result<AiCompletion, PlatformError>`), plus two deterministic test doubles (`AiProvider.fixed(...)`, `AiProvider.failing(...)`), mirroring exactly how `Clock` abstracts "get the time" without needing a real wall-clock library. No vendor SDK, no API key, no concrete provider implementation.

#### Why

Every capability past `provider` needs at least one real provider call to validate against, or a concrete product feature that doesn't exist yet (see spec §4's per-capability blocker table). Building them now would repeat the exact mistake ADR-023 just corrected for Platform Services. `provider`'s interface is the one exception: like `Clock`, it can be fully designed, implemented, and tested without any external dependency, because the abstraction itself — not any concrete backing implementation — is the deliverable.

#### Alternatives considered

- Implement all nine `ai.*` subpackages now: rejected — eight of them have no concrete caller (spec §4).
- Wait until a provider/API key decision is made before writing any AI Runtime code at all: rejected — the interface genuinely needs no credential, and having it ready means the moment a provider is chosen, only a concrete `AiProvider` implementation is needed, not a redesign of the contract every future caller depends on.
- Nest `ai` under `platform`: rejected — the architecture diagram (`CLAUDE.md` §4) places AI Runtime as its own layer, not inside Platform.

#### Trade-offs

`AiProvider`'s contract (`complete(AiPrompt) -> Result<AiCompletion, PlatformError>`) is now a public API surface with no real backing implementation yet — a bet that this is the right shape before any vendor is chosen. Mitigated: it is deliberately minimal (no streaming, no tool-calling parameters, no multi-modal input — all explicitly deferred to `tool`/`mcp`/future revision), so the surface a wrong guess could affect is small.

#### Long-term impact

Connecting a real provider becomes implementing one interface, not designing a contract under deadline pressure once a provider is already chosen. Blocked, explicitly, on a decision that is not mine to make: which provider(s) to integrate, and the API credentials to do so.

## ADR-025

### Repository Intelligence: First Engineering Intelligence Capability, Deliberately Sequenced Ahead of AI Runtime's Remaining Capabilities

#### Decision

Implement the smallest useful slice of Repository Intelligence now — `io.forge.platform.intelligence.repository`: `RepositoryScanner.scan(Path) -> Result<RepositorySnapshot, PlatformError>`, reading one Maven module's `pom.xml` and `src/main/java` tree into a deterministic snapshot (build coordinates, Java version, per-package class counts). No AI, no inference — observed facts only.

`intelligence` is a new top-level package, sibling of `core` and `ai`, representing the "Engineering Intelligence" layer (`CLAUDE.md` §4: `Core → Platform → AI Runtime → Engineering Intelligence → Products`). This is the first code in that layer.

#### Why

Both remaining blockers (GitHub authentication, AI provider/credentials) are genuinely external — neither is something further engineering work resolves. Repository Intelligence's foundational capability (understanding a repository's structure) is explicitly named in the product mission as where "core product differentiation should come from rather than generic chat," and — critically — it needs no AI provider at all: parsing a `pom.xml` and walking a directory tree is deterministic tooling, not a model call. This is genuinely unblocked, real product value, not manufactured busywork.

Sequenced ahead of AI Runtime's remaining capabilities (`router`, `prompt`, `context`, etc. — still correctly blocked per ADR-024's per-capability table) because those need a real provider to validate against, which this doesn't need at all.

#### Alternatives considered

- Wait for the AI provider decision before doing anything further: rejected — leaves genuinely unblocked, valuable work undone for no reason.
- Build a full symbol/dependency graph now: rejected — no concrete use case yet exercises anything beyond package-level facts; richer analysis is deferred until Change Intelligence or Risk Analysis has a real need for it (per the standing instruction not to introduce a large graph schema prematurely).
- Use ArchUnit (already a dependency) as the analysis engine, since it already extracts package/class structure: rejected — it's a test-scope assertion library; repurposing it as a production analysis engine is a scope mismatch, and `pom.xml`/filesystem parsing needs no new dependency at all (JDK's built-in `javax.xml.parsers`).
- Support multi-module recursion now: rejected — no second real module exists in this repository to validate that logic against; a caller wanting a multi-module view calls `scan` once per child module today.

#### Trade-offs

`RepositorySnapshot`'s shape (coordinates, Java version, per-package class counts) is a real public API bet made with only one concrete example (this repository) to validate against. Mitigated by keeping the surface minimal — no speculative fields — and by the type being trivially extensible (a record, not a sealed hierarchy) without breaking existing callers.

#### Long-term impact

Establishes the `intelligence.*` package as the home for all eight Engineering Intelligence sub-capabilities named in `CLAUDE.md` §4 (Repository, Architecture, Code, Performance, Security, Knowledge, Decision, Automation Intelligence) — each gets added the same way, one capability at a time, only when a concrete need justifies it. `RepositoryScanner`'s output is dogfooded against this repository itself in its own test suite — if the scanner and `pom.xml` ever disagree, the test fails immediately, not silently.

## ADR-026

### Architecture Intelligence: First Consumer of Repository Intelligence's Facts

#### Decision

Add `io.forge.platform.intelligence.architecture` — `CycleDetector.findCycles(Set<PackageDependency>) -> Set<CyclicPackageGroup>`, using Tarjan's strongly-connected-components algorithm over the internal-dependency facts `intelligence.repository` (ADR-025) already extracts.

Establishes the dependency direction between Engineering Intelligence sub-capabilities: `architecture` may depend on `repository` (consumes `PackageDependency`); `repository` must never depend on `architecture` (fact-gathering stays independent of analysis built on it). Enforced by a new `ArchitectureTest` rule, not just stated.

#### Why

The prior session's report explicitly flagged that further growth of `intelligence.repository` needed "a second real consumer" to stay justified rather than speculatively accumulating more facts. Circular package dependencies are a genuine, well-understood architectural risk — directly serving the roadmap's Risk Analysis / Architecture Intelligence sub-capability — and the exact input this needs (`Set<PackageDependency>`) already exists from the prior session's work. No AI, no new dependency (plain Tarjan's algorithm, ~100 lines).

#### Alternatives considered

- Naive enumeration of every individual cycle path: rejected — a strongly-connected component of `n` mutually-reachable packages can contain exponentially many distinct paths through it; reporting the entangled group is the actionable fact, not an exhaustive path listing.
- Depth-first "detect first cycle and stop": rejected — a real codebase can have multiple independent cyclic clusters; stopping at the first gives an incomplete, misleading picture.
- Iterative (non-recursive) Tarjan implementation: rejected for now — recursion is fine for realistic repository package counts (tens to low hundreds); documented as a known scaling limit to revisit only if a real graph ever needs it, not built preemptively.

#### Trade-offs

Recursive implementation has a real (if distant) stack-depth ceiling on pathologically large graphs. Accepted and documented rather than solved speculatively.

#### Long-term impact

Dogfooded immediately: this repository's own package graph, scanned by its own tooling, is verified acyclic — the first real, non-trivial claim `intelligence.*` has produced about this codebase. Establishes the pattern for future Engineering Intelligence capabilities: `repository` gathers facts, other `intelligence.*` sub-capabilities analyze them, one-directional.

## ADR-027

### First Products-Layer Code: A Minimal CLI

#### Decision

Add `io.forge.platform.cli` — `RepositoryIntelligenceCli` (a Spring `CommandLineRunner`, gated behind an explicit `scan` command) and `RepositoryIntelligenceReport` (framework-free rendering logic, independently tested). Usage: `java -jar forge-ai.jar scan [path]`. First code in the "Products" layer (`CLAUDE.md` §4: `Core → Platform → AI Runtime → Engineering Intelligence → Products`).

#### Why

Three sessions had built `intelligence.repository` and `intelligence.architecture` — real, tested capabilities — but nothing made them runnable or visible to an actual human; the only way to see their output was reading unit test assertions. The project's own completion definition explicitly requires "usable interface/API/CLI" before Forge can be called demonstrable, and a CLI is the smallest such surface — no web layer, no new framework dependency (`CommandLineRunner` is already part of `spring-boot-starter`), no AI, no GitHub access needed.

#### Alternatives considered

- A REST API / web layer: rejected — would require adding `spring-boot-starter-web`, plus Validation/Serialization (Platform Services, still correctly deferred per ADR-023) to do properly; far more than the smallest justified surface for "make existing output visible."
- Leave it CLI-invisible until a bigger product decision is made: rejected — this is exactly the kind of avoidable non-decision the project's own rules warn against ("do not stop for avoidable reasons"); a CLI needed no product-direction decision at all.
- An unconditional `CommandLineRunner` (scan on every startup): implemented first, then rejected on self-review — fires on every Spring context boot, including this application's own tests (observed directly: a report printed during `ForgePlatformApplicationTests`). Harmless today (single-purpose app) but a real landmine once this application gains a second purpose (a web layer, a long-running service) that shouldn't be killable by an unrelated startup scan failing. Fixed by gating behind an explicit `scan` first argument before this ever shipped.

#### Trade-offs

`RepositoryIntelligenceReport`'s text output format is now a public-facing contract (of sorts) with exactly one real caller (this session's own tests) validating it. Low risk: it's plain human-readable text, not a machine-parsed format, and trivially changeable.

#### A real gap this decision surfaced

Documenting `java -jar forge-ai.jar scan [path]` as the usage line, then actually running it (rather than assuming a Javadoc comment is correct) found that the built jar had no runnable manifest at all — `spring-boot-maven-plugin` was never configured in `pom.xml`, so `mvn package` produced a plain, non-executable jar. Every prior session's `mvn clean verify` passed regardless, because nothing had ever tried to *run* the packaged artifact — only compile and test it. Fixed by adding the plugin (inheriting version/execution from `spring-boot-starter-parent`); a first attempt double-repackaged (an explicit execution block conflicting with the parent's already-managed default execution) — caught by reading the build log, not assumed correct, and simplified to a bare plugin declaration. Both the jar and `spring-boot:run` invocations re-verified working end-to-end after the fix.

#### Long-term impact

Establishes the "Products" layer's dependency shape: `cli` may depend on everything below it (`core`, `ai`, `intelligence`); nothing may depend on `cli` — enforced by `ArchitectureTest`, not just documented. Future product surfaces (a real API, a dashboard) follow the same one-directional shape.

## ADR-028

### Multi-Module Repository Scanning and Maven Parent Inheritance

#### Decision

`RepositoryScanner` gains `scanWorkspace(Path)`, scanning a parent module plus every module its `<modules>` declares, and `scan(Path)` now resolves `groupId`/`version`/`java.version` inherited from a local `<parent>` when a module doesn't declare them directly. `RepositoryIntelligenceReport` (the CLI) always calls `scanWorkspace`, since a single-module project's parent simply has no declared `<modules>` and naturally produces a one-element list — single- and multi-module targets are one code path, not two.

#### Why

ADR-025 explicitly deferred multi-module support: "no second module exists to validate against." That constraint no longer holds — DLMP, a real, independent, 8-module Spring Boot project already present on this machine, is exactly such a fixture. Designing against it (read-only; DLMP itself was never modified) surfaced a real requirement the original design missed entirely: properly-structured multi-module Maven projects have child modules that inherit `groupId`/`version`/`java.version` from their parent rather than redeclaring them — confirmed directly (`loan-service/pom.xml` declares only `artifactId`). Without handling this, the scanner would fail on every child module of any real multi-module project, which is the common case, not an edge case.

Validated end-to-end against DLMP's full workspace (`java -jar forge-ai.jar scan <dlmp-root>`): correctly scanned all 8 modules (parent + 7 services), correctly inherited coordinates and Java 21 for every child, and found a genuine circular dependency in `loan-service` (`command` ↔ `saga`) — a real architectural finding about a real, independently-developed codebase, not a synthetic test case.

#### Alternatives considered

- Full Maven-model resolution (properties interpolation, dependency management inheritance, profiles): rejected — far beyond what "understand a repository's structure" needs; the common single-level local-parent case (verified against DLMP) covers the realistic scenario without building a Maven implementation.
- Committing DLMP itself as a test fixture, or writing an automated test with DLMP's absolute path: rejected — would break reproducibility on a clean checkout or CI runner, where a sibling repository does not exist. Verified manually against DLMP for real-world confidence; the committed test suite uses only synthetic fixtures reproducing DLMP's exact shape.
- Partial-success semantics for `scanWorkspace` (report successes and failures per module): rejected for now — fail-fast is simpler and has no concrete caller needing partial results yet; revisit if one appears.

#### Trade-offs

`scanWorkspace` does not recurse into nested `<modules>` (multi-level aggregation) — no real project on hand needs that yet either; single-level covers DLMP and is extensible later without a breaking change.

#### Long-term impact

Forge can now produce real findings about real, independent, multi-service systems — not just about itself. This is the first evidence the Engineering Intelligence foundation generalizes beyond its own repository, which is the actual bar "Engineering Model" work needs to clear before going further.

## ADR-029

### First Reasoning Capability: `RepositoryAssessor`

#### Decision

Add `io.forge.platform.reasoning` — `RepositoryAssessor.assess(RepositorySnapshot, AiProvider) -> Result<ArchitectureAssessment, PlatformError>`, the first capability combining Engineering Intelligence facts (`intelligence.repository`/`intelligence.architecture`) with the AI Runtime (`ai.provider`). `ArchitectureAssessment` keeps deterministic `evidence` (facts) and AI-generated `narrative` (inference) as separate fields, never merged, so a caller can always tell which is which.

New top-level package, sibling of `core`/`ai`/`intelligence`/`cli`. May depend on `ai`, `intelligence`, and `core`; nothing in those three may depend on it (enforced by `ArchitectureTest`, not just documented), and it may never depend on `cli`.

#### Why

This is the "Reason" stage of the product's stated pipeline (Repository → Understand → Analyze → Risk → **Reason** → Plan → ...), and the AI Runtime rules explicitly sanction building it now with a deterministic test double standing in for a real provider, so the rest of the product isn't blocked on a provider decision that remains genuinely outside this session's authority.

#### Alternatives considered

- Wire this into the CLI's default `scan` output using `AiProvider.fixed(...)`: rejected on self-review. A fixed provider always returns the same canned string regardless of the actual findings — presenting that as if it were real AI-generated insight would be actively misleading, not "provider-neutral design." The interface's whole purpose is testing the plumbing, not producing user-facing output that impersonates real reasoning. `RepositoryAssessor` is built, fully tested, and ready to wire into the CLI the moment a real provider exists, or wired in now with explicit, honest labeling that no real AI is behind it — deferred as a product-presentation choice, not an engineering blocker.
- Put this logic inside `intelligence.*`: rejected — `intelligence` is deliberately deterministic, AI-free analysis (ADR-025's own framing); mixing in an AI Runtime dependency would blur that boundary for every existing and future `intelligence.*` capability, not just this one.
- Put this logic inside `ai.*`: rejected — `ai` is the provider abstraction itself; consuming Engineering Intelligence facts is a different concern layered on top of it, not part of the abstraction.

#### Trade-offs

The prompt text `RepositoryAssessor` builds is now a real, if informal, contract — get it wrong and a future real-provider swap needs rework. Mitigated by keeping it simple (plain evidence lines, one instruction sentence) and validating its shape via a capturing test double, not just trusting it compiles.

#### Long-term impact

Establishes the pattern for every future capability that needs both Engineering Intelligence facts and AI reasoning: depend on both, stay out of `cli`, keep evidence and inference visibly separate. The product's core pipeline shape (Understand → Analyze → Reason) now exists in code, end to end, even though the "Reason" stage's real intelligence is still pending a provider decision.

## ADR-030

### Engineering Model: `intelligence.model`

#### Decision

Add `io.forge.platform.intelligence.model` — `EngineeringModel` (modules + inter-module dependencies, with a `dependentsOf(artifactId)` traversal), `ModuleDependency`, and `EngineeringModelBuilder`. `RepositorySnapshot` gains a fifth field, `declaredDependencyArtifactIds`: the raw, unresolved set of artifactIds each module's own `pom.xml` declares.

Resolution is deliberately split: `RepositoryScanner` collects the raw fact (it cannot know whether a given artifactId is a sibling module or an external library), and `EngineeringModelBuilder` resolves it against the workspace's full module set. This is the identical deferred-resolution pattern already used for `PackageDependency` (ADR-028) — reused, not reinvented.

#### Why

The roadmap's "Engineering Model / Engineering Knowledge" stage needs a representation of a repository *as a system*, not just a bag of independent modules. The concrete question motivating it — "if module X changes, what else in this workspace could be affected?" — is the direct prerequisite for Change Intelligence and Risk Intelligence, the next two roadmap stages.

Validated against DLMP's real 8-module workspace: correctly produced all six real `service -> common` dependencies, correctly excluded external libraries (Spring Boot starters, Kafka, Resilience4j etc.), and correctly excluded `common`'s own `<artifactId>` self-reference. Cross-checked independently against DLMP's actual `pom.xml` files rather than trusting the output looked plausible.

#### Alternatives considered

- A general-purpose node/edge graph with a query engine: rejected outright — the standing instruction is explicit ("do not introduce a huge generic knowledge graph", "start with the simplest representation"). Two typed lists answer the actual question; a graph engine answers hypothetical ones.
- Persistence (embedded database, serialized model): rejected — nothing yet needs the model to outlive a single process. Adding storage now would be infrastructure ahead of a use case.
- Full Maven dependency resolution (transitive external dependencies, version conflicts, scopes): rejected — that is Maven's job, and the product question here is about *workspace* structure, not the full external dependency tree.
- Inferring service-to-service HTTP relationships from configuration (DLMP's `api-gateway` routes to five services it has no Maven dependency on): deliberately deferred, and `ModuleDependency`'s Javadoc says so explicitly rather than leaving the omission silent. That is a real, different relationship kind; conflating it with build-time coupling would make the model lie about what it knows.

#### A real bug this decision surfaced

The first implementation used `getElementsByTagName("dependencies")` to find a module's dependency list. That searches the entire document — so for a pom with only a `<dependencyManagement><dependencies>` block and no top-level `<dependencies>` (exactly DLMP's root pom's shape), it would have silently reported version-pinned management entries as real applied dependencies. Caught by checking a real `pom.xml` before shipping, not after. Fixed by resolving only `<project>`'s *direct* children, and the same latent bug was audited and fixed in the two other places using the same pattern (`readModuleNames`, `readJavaVersionFromOwnProperties` — the latter would have picked up a `<profile>`'s properties block).

#### Long-term impact

Change Intelligence ("what does this change affect?") and Risk Intelligence ("how risky is it?") now have a real structure to reason over. The model stays honest about its own limits: it represents build-time coupling only, and says so.

## Why I changed the roadmap

After reflecting on everything we've designed, I think many portfolio projects fail because they optimize for features.

Senior engineers optimize for decision quality.

The difference is:

Junior mindset: "What should we build next?"
Senior mindset: "What decision today reduces complexity for the next five years?"

That's the mindset I want this project to demonstrate.

## Before Step 8

I want to make one final architectural adjustment.

We've now defined what Forge AI is.

The next document should define how engineers contribute to it.

Not coding.

Not APIs.

Not databases.

Instead, we'll create a Definition of Done + Development Lifecycle document. That document will define exactly how every feature moves from an idea to production quality. Once that's in place, GitHub Copilot (or any AI coding assistant) will have a repeatable process to follow for every module, making the generated code much more consistent and maintainable over time.
