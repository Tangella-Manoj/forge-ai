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
