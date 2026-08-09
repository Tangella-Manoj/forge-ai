# CLAUDE.md — Forge AI Platform Engineering Constitution

**Status:** Authoritative
**Scope:** Entire repository
**Audience:** Any AI assistant (Claude Code, Copilot, Gemini, Cursor, ChatGPT, or future tools) and any human contributor joining this project.

This document is self-sufficient. A reader should not need prior conversation history to contribute correctly. Where this document summarizes another `docs/` file, that source file remains the detailed reference; this document is the fast, authoritative entry point and the tie-breaker when documents disagree (see §0).

---

## 0. How to Use This Document

Read this file before touching any code or documentation. It consolidates and resolves guidance spread across `docs/01`–`13`, `ARCHITECTURE_STATUS.md`, `README.md`, `CONTRIBUTING.md`, and `.github/copilot-instructions.md`. Where those documents conflict with each other, **this document states the resolved, current answer**, and notes what it supersedes. Where they agree, this document is the condensed version — consult the linked source for full rationale (ADRs, alternatives considered, trade-offs).

This document does not invent new architecture. Every rule here traces to an existing decision in `docs/`. Where a genuine gap exists (nothing decided yet), it is marked **Undecided** rather than filled in with a guess.

---

## 1. Project Mission

Reduce the time engineers spend understanding, debugging, reviewing, documenting, and maintaining software — while preserving engineering quality, security, and reliability. AI is the mechanism; better engineering outcomes are the goal. (Source: `01_PROJECT_VISION.md`, `02_PRODUCT_REQUIREMENTS.md`)

## 2. Project Vision

Forge AI Platform is an AI-native Engineering Intelligence Platform. The product name is **Forge AI Platform**; the underlying architecture and platform identity is **OpenEIOS** (Open Engineering Intelligence Operating System) — the same relationship as Android/Pixel or Chromium/Chrome. The core module specifically (kernel spec, core coding guidelines) is referred to as **Forge Platform Core**. It is explicitly **not** a chatbot, not a Copilot/Cursor/IDE clone, and not a generic RAG demo. It helps engineers design, build, review, secure, deploy, monitor, understand, and continuously improve software systems by combining repository analysis, observability, knowledge management, and AI reasoning into evidence-backed, explainable, human-approved recommendations. (Source: `01_PROJECT_VISION.md`, `04_DOMAIN_MODEL.md`)

## 3. Long-Term Goals

Version roadmap (product-level, from `02_PRODUCT_REQUIREMENTS.md`):

| Version | Focus |
|---|---|
| V1 | Engineering Intelligence Core |
| V2 | Observability Platform |
| V3 | AI Workflow Automation |
| V4 | Autonomous Engineering Assistant |
| V5 | Enterprise Engineering Platform |

Definition of success (from `ARCHITECTURE_STATUS.md`): a flagship reference architecture for AI-native engineering platforms built with Java — maintainable, credible to a senior engineer, and viable as an open-source ecosystem or commercial foundation. The permanent litmus test for any feature: *"Would a senior engineer at a top product company consider this maintainable, scalable, secure, and production-ready?"* If no, redesign.

## 4. Architecture Philosophy

**Authoritative layering** (from `ARCHITECTURE_STATUS.md`, the most current architecture document):

```
Core → Platform → AI Runtime → Engineering Intelligence → Products
```

- **Core** — pure business abstractions (Typed IDs, `Result<T,E>`, Error Model, Domain Events, Clock, Value Objects). No framework, no AI, no Spring dependency.
- **Platform** — cross-cutting engineering capabilities (validation, configuration, logging, serialization, observability, security foundations).
- **AI Runtime** — provider-independent AI infrastructure (provider abstraction, model router, prompt/context engines, tool calling, MCP, memory, evaluation, guardrails). This is the only layer permitted to know about a specific AI vendor.
- **Engineering Intelligence** — the product capabilities (Repository, Architecture, Code, Performance, Security, Knowledge, Decision, Automation Intelligence).
- **Products** — user-facing surfaces (Dashboard, REST API, CLI, VS Code extension, SDKs).

This supersedes the two earlier, narrower layering sketches in `03_SYSTEM_ARCHITECTURE.md` ("Users → Web Dashboard → API Gateway → Forge Core Platform → Infrastructure → AI Providers") and `10_PLATFORM_CAPABILITY_MAP.md` ("Foundation → Reasoning → Engineering → Automation → Products") — those remain useful for their more granular module/capability detail, but `ARCHITECTURE_STATUS.md`'s five-layer model is the one to cite for the overall shape of the system.

**Project type:** Modular Monolith → Modular Services evolution (ADR-001). One deployable application today; every module designed so it *could* become an independent service later without changing its business logic. This is the litmus test for whether a module's design is sound: *"Can this module become an independent service without changing its business logic?"* If no, redesign.

**Architecture status: FROZEN** (as of 03-Aug-2026, `ARCHITECTURE_STATUS.md`). Foundational architecture is stable. Future architectural evolution happens through ADRs and implementation feedback — never speculative redesign. No new foundational architecture documents without a significant engineering reason.

## 5. Repository Philosophy

A repository should be understandable within five minutes: where code, docs, architecture, APIs, tests, prompts, and workflows live, without asking. Every folder answers exactly one question — never `utils/`, `misc/`, `helpers/`, `common2/`, `temp/`. Structure reflects business capabilities, not framework layers (`Identity/`, `Repository/`, `Knowledge/` — not `controller/`, `service/`, `dao/`). Inside each business area, framework layering (`controller`, `application`, `domain`, `infrastructure`) is fine — vertical-slice-first, framework-layered-second.

Never abbreviate names (`Identity Service`, not `authsvc`; `Repository Service`, not `repo2`). Every folder should eventually carry its own README. The target top-level structure (`platform/ applications/ services/ sdk/ agents/ prompts/ workflows/ infrastructure/ scripts/ testing/ examples/ assets/ tools/`) is documented in `05_REPOSITORY_BLUEPRINT.md`; the repository has not yet grown into it — the current single Maven module (`src/main/java`, `src/test/java`) is expected and correct for this stage, not a violation.

## 6. Engineering Principles

**Principle Zero:** Forge never optimizes for automation. Forge optimizes for better engineering decisions.

1. Evidence before automation.
2. Architecture before implementation.
3. Business capability before framework.
4. Reasoning before generation.
5. Engineering judgment is the product; AI is the tool.
6. Every recommendation must be explainable.
7. Every architectural decision must be reversible unless explicitly documented otherwise.
8. Humans remain accountable; AI remains advisory.
9. Delete complexity before adding intelligence.
10. Every module must increase engineering confidence, not engineering excitement.

(Source: `11_ENGINEERING_PRINCIPLES.md`)

## 7. Design Principles

**The Five Laws** (from `06_ENGINEERING_STANDARDS.md`):

1. Code is written for humans; future engineers must understand it in minutes.
2. If code needs comments to explain *what* it does, the design is probably wrong. Comments explain *why*, never *what*.
3. Every class has one responsibility. If its description needs "and," split it.
4. Every public API must be stable; breaking changes require versioning.
5. Everything must be testable. If something can't be tested, its design is wrong.

**Mandatory:** SOLID, Clean Architecture, Domain-Driven Design, Vertical Slice Architecture.
**Preferred:** Event-Driven Architecture (when asynchronous behavior exists), composition over inheritance, immutable objects wherever practical.

We optimize for readability, maintainability, simplicity, scalability, reliability — never for clever code.

## 8. Package Dependency Rules

From `13_CORE_CODING_GUIDELINES.md` §10, currently enforced by code review (no automated architecture test exists yet — see §33/§40):

- `core` depends on nothing outside the Java standard library, and framework/third-party annotations only if truly unavoidable.
- `platform` may depend on `core`. Never the reverse.
- Domain/feature modules depend on `core` and `platform`. Never the reverse.
- No framework dependencies inside `core` packages.

Domain-level dependency rule (`04_DOMAIN_MODEL.md`): `UI → Application → Domain → Infrastructure`, never `Infrastructure → Domain`. Cross-domain communication happens only through interfaces, events, or contracts — never direct table/data access across domain boundaries (e.g., Analytics must never query another domain's database directly).

## 9. Public API Policy

- Every public core API is a long-term contract (`13_CORE_CODING_GUIDELINES.md`, kernel spec §12). Follow semantic versioning: major = breaking, minor = backward-compatible addition, patch = backward-compatible fix.
- Do not remove a public core API without deprecation first, removal later, on a major version.
- Keep public surfaces minimal. Prefer hiding implementation types behind an interface's static factories over exposing concrete classes (established pattern: `InternalUuidGenerator` is package-private behind `TypedId`'s subtypes; `Clock`'s system/fixed implementations are package-private behind `Clock.system()`/`Clock.fixed()`).
- Do not create an interface for a single, unlikely-to-vary implementation (`13_CORE_CODING_GUIDELINES.md` §3) — but do create one when substitution is a real, foreseeable need (e.g., `Clock` — system vs. fixed vs. future strategies).
- Before adding any public method, ask: does a real, current caller need this, or is it speculative? Speculative additions are rejected (see §37, Refactoring Policy, and the project's general anti-scope-creep stance).

## 10. Coding Standards

From `06_ENGINEERING_STANDARDS.md`:

- **Spring Boot:** constructor injection only, never field injection (`@Autowired` on a field). Never expose entities directly — use DTOs. No business or database logic inside controllers; controllers only receive, validate, delegate, respond.
- **Database:** every table needs a UUID primary key, `created_at`, `updated_at`, version, and soft-delete/audit metadata where appropriate. Never `SELECT *`. Indexes must be intentional. Every migration must be reversible. *(Not yet applicable — no persistence layer exists yet.)*
- **API:** REST follows OpenAPI. Errors follow RFC 9457 (Problem Details for HTTP APIs) — never bare `{"status":"failed"}`. *(Not yet applicable — no REST layer exists yet.)*
- **Logging:** never `System.out.println`. Structured logging only. Every log line carries request ID, correlation ID, timestamp, service name. *(Not yet applicable — no logging framework wired up yet.)*
- **Exceptions:** never catch bare `Exception`. Catch specific exceptions. Every exception carries meaning, context, and a clear action. Exceptions are for truly exceptional, unrecoverable, or infrastructure-level failures only — never for normal control flow (see §17–18).
- **Git:** never commit a broken build, temporary code, commented-out code, generated files, or credentials. Every commit builds successfully. Conventional Commits format: `feat(identity): add JWT authentication`, `fix(repository): resolve cache invalidation bug`.

## 11. Java Guidelines

**Java 25** is the current, authoritative target — matching `pom.xml` (`<java.version>25</java.version>`), `.github/workflows/ci.yml`, `.github/copilot-instructions.md`, `06_ENGINEERING_STANDARDS.md`, and ADR-002. All Java-version references across the repository are consistent as of the Sprint 1 housekeeping milestone (Aug 2026).

Prefer: records, sealed classes/interfaces, pattern matching, virtual threads, structured concurrency, modern streams. Avoid legacy patterns without justification.

**Records vs. classes** (`13_CORE_CODING_GUIDELINES.md` §1): prefer `record` for immutable value carriers with no identity and minimal behavior. Use `final class` only when invariants, lazy computation, or custom methods justify it. Never use mutable classes for core primitives.

**Sealed types** (§2): use for closed hierarchies with a known, stable set of subtypes — `Result` (`Success`/`Failure`), `PlatformError` (`DomainError`/`InfrastructureError`). Do not seal an interface meant for open-ended extension (`Clock` is intentionally *not* sealed — see its own guidance in §22).

## 12. Documentation Standards

- Every public class states its purpose (Javadoc).
- Every module gets a README once it has enough content to need one.
- Every architecture change requires an ADR (`07_ARCHITECTURE_DECISIONS.md`).
- `docs/INDEX.md` is the canonical, current index of all foundational documents — keep it current when new docs are added; other documents (like this one, and `README.md`) may reference it but should not be treated as more current than it.
- Documentation is part of the product, not an optional task (ADR-016).

## 13. Testing Standards

Every feature requires unit tests; integration, contract, and architecture tests where applicable. Bug fixes require regression tests. Use Testcontainers for integration tests against external dependencies once such dependencies exist. Architecture tests (enforcing package dependency rules, §8) are called out as "especially important" (ADR-012) but do not yet exist in this repository — this is a known, tracked gap (see §40).

## 14. Performance Guidelines

Measure, never guess (ADR-015). Every optimization must be backed by profiling, benchmarking, or metrics evidence — never assumption. Performance is considered as part of Definition of Done (§24), not bolted on afterward.

## 15. Thread Safety Rules

Core types are thread-safe by design: immutable by default, no shared mutable state, no hidden caches unless explicitly designed and documented, side-effect-free helpers where possible (kernel spec §10). This is a hard requirement for anything in `core` — every kernel primitive built so far (`Result`, `TypedId` implementations, `PlatformError`/`DomainError`/`InfrastructureError`) satisfies it via immutability; any new kernel type must too.

## 16. Security Guidelines

Secure by default (ADR-014) — every feature assumes hostile input. Never trust client input: validate everything, escape everything, sanitize everything. Never store secrets, passwords, or tokens in code. Every module must eventually support authentication, authorization, RBAC, audit logging, secrets management, secure API design, input validation, and rate limiting (`02_PRODUCT_REQUIREMENTS.md`) — none of this exists yet, as no Identity/Security module has been built (expected at this stage; tracked on the roadmap, §29).

## 17. Error Handling Philosophy

Exceptions are reserved for truly exceptional, unrecoverable, or infrastructure-level failures — never for expected business outcomes (`13_CORE_CODING_GUIDELINES.md` §4). Expected failure paths use `Result<T,E>` with a typed error. This gives explicit, reviewable, testable failure semantics instead of hidden control flow via `throws`.

## 18. Result Pattern Usage

`Result<T, E>` (`io.forge.platform.core.result.Result`) is implemented as a sealed interface with `Success<T,E>`/`Failure<T,E>` record variants. Rules:

- Success and failure values are explicit and non-null (`Result.success(null)` and `Result.failure(null)` both throw `NullPointerException`).
- Use `map`/`mapError`/`flatMap`/`fold` to compose outcomes without unwrapping early.
- Avoid nesting `Result<Result<T,E>, E>` unless a higher-level API genuinely needs it — it is supported (tested) but not the default shape to reach for.
- `E` should generally be `PlatformError` (or one of its subtypes) for core/platform code (§17, §19 below) — this pairing is why `PlatformError` was built immediately after `Result`.

## 19. Typed ID Guidelines

Every domain identity is a strongly typed, immutable value object wrapping a UUIDv7 (kernel spec §2, `13_CORE_CODING_GUIDELINES.md` §6):

- Implement `TypedId` (`UUID value()`).
- Compact constructor validates non-null and UUIDv7-ness (`version() == 7 && variant() == 2`), throwing `NullPointerException`/`IllegalArgumentException` — not `Result` — since this is a constructor-level invariant violation, not an expected business outcome (consistent with §17).
- Equality compares by wrapped UUID value only.
- Never expose a raw `UUID` across a public core API when a typed ID exists for that concept.
- Generation is delegated to a package-private strategy (`InternalUuidGenerator`) so the concrete UUIDv7 algorithm can be replaced later (e.g., by a JDK-native or library implementation) without touching any typed ID's public contract.

Implemented so far: `WorkspaceId`, `RepositoryId`, `DecisionId`, `AnalysisId`, `EventId`. New typed IDs follow this exact template — one record per identity concept, same four members (compact constructor, `newId()`, `of(UUID)`).

## 20. Value Object Guidelines

**Resolved by engineering decision; no dedicated artifact.** Per kernel spec §7, a shared kernel-level value-object base type is warranted "only if it demonstrably adds consistency," not by default. The pattern (immutable records, constructor-validated, fail-fast) is already fully demonstrated by `TypedId`'s and `PlatformError`'s implementations. No concrete domain value object exists yet to build against — building a placeholder would be a speculative abstraction. Revisit once a real domain module (Identity, Workspace, etc.) has a concrete value object need; at that point, propose its API first (§29) as usual.

## 21. Domain Event Guidelines

**Implemented.** `io.forge.platform.core.event.DomainEvent` — an interface (not sealed; concrete domain events are an open-ended, growing set, unlike `Result`/`PlatformError`) exposing `eventId()` (`EventId`), `occurredAt()` (`Instant`, must come from `Clock`, never a direct time call), `aggregateId()` (`TypedId` — deliberately non-generic; see `PROJECT_STATE.md` for the trade-off this accepts), and `version()` (`long`). No framework binding; a future `IntegrationEvent` handles transport boundary-crossing when that need becomes concrete.

## 22. Clock Usage Rules

**Implemented.** Package `io.forge.platform.core.time`. Public surface: the `Clock` interface (`Instant now()`, annotated `@FunctionalInterface`) plus two static factories, `Clock.system()` and `Clock.fixed(Instant)`. Implementations (`SystemClock`, `FixedClock`) are package-private, reached only through those factories. `SystemClock.now()` is the one sanctioned place in `core` that calls `Instant.now()` directly — it *is* the abstraction boundary, not a violation of it.

- **No direct `Instant.now()` (or any direct time call) inside core logic, ever.** All time-sensitive logic takes a `Clock` as a dependency.
- `Clock` is intentionally *not* sealed — clock strategies are open-ended (future: offset clocks, request-scoped frozen clocks), unlike `Result`/`PlatformError`, which model closed business concepts.
- Tests use `Clock.fixed(instant)` for determinism — never rely on real elapsed time or `Thread.sleep` to assert time-based behavior.

## 23. Validation Rules

**Not yet implemented.** Per kernel spec §8, two distinct layers, not to be conflated:

- **Jakarta Validation** — for external input, DTOs, and request boundaries.
- **Kernel-level validation** — for shared invariant checks on IDs, value objects, and other platform primitives (this already happens today, ad hoc, inside each typed ID's and error type's compact constructor; a dedicated Validation task will formalize shared helpers for this, not replace the existing constructor-level checks).

## 24. Definition of Done

A feature is done only when all of the following are true (`06_ENGINEERING_STANDARDS.md`, `08_ENGINEERING_DEVELOPMENT_LIFECYCLE.md`):

Requirements implemented · Tests passing · Documentation updated · API documented · Security reviewed · Logging added · Metrics added · Error handling completed · Performance considered · Code reviewed · Build successful.

Otherwise: **not done**, regardless of how much code exists.

## 25. Definition of Review

Every review answers (`08_ENGINEERING_DEVELOPMENT_LIFECYCLE.md` Stage 8):

Is the architecture correct? · Is naming clear? · Is complexity justified? · Can another engineer understand this? · Can AI understand this? · Can this scale?

Architecture is reviewed before syntax — correct architecture beats beautiful syntax (`06_ENGINEERING_STANDARDS.md`).

## 26. Pull Request Checklist

Every PR answers: What problem does this solve? Why is this solution chosen? What alternatives were rejected? How was it tested? How does it affect architecture? What risks remain? How can it be rolled back? (Consolidated from `06_ENGINEERING_STANDARDS.md`, `08_ENGINEERING_DEVELOPMENT_LIFECYCLE.md`, `CONTRIBUTING.md` — all three state the same checklist; this is the single copy to maintain going forward.)

At least one approving review is required for non-trivial changes. CI (build, tests, linters where configured) must pass. No breaking the build on `main`.

## 27. Architecture Decision Rules

An ADR is required for any decision that answers "why this, why not the alternatives, what trade-off did we accept" in a way future contributors (human or AI) need in order to safely change it later — new core technology, a new architectural pattern, a reversal of an existing ADR, or a cross-module boundary change. Small, local implementation choices within an already-decided pattern do not need a new ADR.

## 28. ADR Policy

ADRs live in `docs/07_ARCHITECTURE_DECISIONS.md` today (numbered ADR-001 through ADR-020). `ARCHITECTURE_STATUS.md` proposes a future `docs/ADR/` directory with one file per decision (ADR-001 "Use UUIDv7," etc.) for when the single-file format becomes unwieldy — that migration has not happened yet; continue appending to the existing numbered list in `07_ARCHITECTURE_DECISIONS.md` until a decision is made to split it.

## 29. Roadmap Governance

Work is organized into three tracks, kept deliberately separate so they never compete for priority:

- **Track A — Platform Core** (Sprint 1, the only track currently active): `Result<T,E>` ✅ · Typed IDs ✅ · `PlatformError` ✅ · `Clock` ✅ · Value Objects ✅ (resolved by decision, no artifact) · Domain Events ✅ · Validation 🛑 blocked (see `PROJECT_STATE.md`) · Core Tests (not started).
- **Track B — Engineering Infrastructure**: developer/AI tooling and process — this document, `AI_CONTEXT.md`-style continuity aids, ADR directory migration, CI improvements, release automation, doc-consistency cleanup. Backlog only; never displaces Track A work.
- **Track C — AI Runtime**: future; not started; comes after Track A (Platform Core) and the subsequent Platform Services / Identity / Workspace sprints per `ARCHITECTURE_STATUS.md`'s sprint plan.

**Two-stage discipline for every public API in Track A:** (1) API proposal — package structure, interfaces, public classes, factory methods, thread safety, testability, justification, alternatives considered, exclusions, future extensibility — reviewed and approved *before* (2) implementation. Do not implement before the API stage is explicitly approved.

Full sprint sequence remains as documented in `ARCHITECTURE_STATUS.md`: Sprint 0 (Foundation, done) → Sprint 1 (Platform Core, in progress) → Platform Services → AI Runtime → Identity → Workspace → Repository Intelligence → Knowledge Engine → Reasoning Engine → Decision Engine → Automation Engine → Engineering Intelligence → Dashboard → v1.0.

## 30. AI Assistant Rules

Any AI assistant working in this repository — regardless of vendor — follows the same rules a human contributor does, plus:

- Read relevant `docs/` (and this file) before generating code.
- Understand the domain and respect the frozen architecture (§4) before proposing changes.
- Follow the two-stage API-review-then-implementation discipline (§29) for any new public core API.
- Stop and wait for explicit approval at the boundaries the requester defines (after a plan, after an API proposal, after an implementation) — do not chain forward into the next task unprompted.
- Generated code must always be reviewed, tested, and understood by a human before merging (ADR-018) — AI producing code is not itself sufficient for that code to be considered trustworthy.

## 31. What AI May Do

Generate code following the standards in this document · propose architecture-consistent designs and API surfaces · write and run tests · run formatting and build verification · propose ADRs · identify inconsistencies, risks, and technical debt · ask clarifying questions when a decision is genuinely ambiguous or architecture-affecting · install/configure local developer tooling needed to build and verify the project (e.g., a JDK version) with transparency about what was done and why.

## 32. What AI Must Never Do

Generate random or placeholder files · invent architecture not grounded in existing decisions · ignore coding standards · skip tests or documentation · modify production systems autonomously · silently resolve genuine architectural ambiguity by guessing instead of asking · treat frozen architecture as open for redesign without a strong, stated engineering reason and an ADR · commit secrets, credentials, or broken builds · take destructive git actions (force-push, hard reset, history rewrite) without explicit, scoped user approval · continue past a stop point the user has set.

## 33. Self-Review Checklist

Before any core code is considered ready for review, it must answer (`13_CORE_CODING_GUIDELINES.md` "Default Review Check," extended):

Is this immutable by default? · Does it preserve typed safety? · Does it avoid framework coupling? · Does it keep equality semantics correct? · Does it keep failure handling explicit? · Why is this implementation correct? · What trade-offs were made? · What alternatives were considered? · Is the public API minimal? · Would a Principal Engineer approve this as-is? · What would be improved in a future version?

If the answer to any correctness/safety question is no, redesign before merging — do not defer to a later fix.

## 34. Repository Maintenance Rules

- `docs/INDEX.md` is updated whenever a new foundational document is added.
- `.github/CODEOWNERS` reflects actual ownership as maintainers change.
- Abandoned artifacts (old packages, dead branches of an earlier naming attempt, broken tool configs) are cleaned up deliberately, not left staged indefinitely — if one is found, it is called out explicitly and a decision is made about it rather than silently carried forward or silently deleted.
- Toolchain version pins (build plugins, JDK) are checked for compatibility with each other before bumping any one of them — a JDK upgrade can silently break formatter/coverage tooling pinned to older internals.

## 35. Versioning Rules

Semantic versioning for the Platform Core (kernel spec §12): major = breaking API change, minor = backward-compatible addition, patch = backward-compatible fix. Deprecate before removing; preserve existing behavior unless a breaking change is deliberate and documented (ADR required, §27).

## 36. Release Philosophy

Every module carries exactly one status at a time: Experimental, Alpha, Beta, Stable, or Enterprise Ready (`08_ENGINEERING_DEVELOPMENT_LIFECYCLE.md`). A release additionally verifies: backward compatibility, database migrations (once they exist), feature flags (if needed), monitoring, rollback strategy, and a deployment checklist.

## 37. Refactoring Policy

Prefer deleting code over adding code. Prefer simple architecture over clever architecture. Prefer explicit design over hidden magic. Prefer long-term maintainability over short-term speed. Technical debt is never hidden — every shortcut is recorded with its reason, impact, owner, and target removal version.

## 38. Escalation Rules

Never guess on genuine ambiguity. Present trade-offs, recommend one option, and wait for approval when a decision affects architecture, introduces a new dependency, changes a public API, or touches a shared/tracked file in a way that affects other contributors. Reversible, purely local, non-architectural implementation details do not need escalation — use judgment and proceed, but be ready to explain the choice in review (§25, §33).

## 39. Project Lifecycle

Every feature passes through ten mandatory stages, regardless of who (human or AI) writes it (`08_ENGINEERING_DEVELOPMENT_LIFECYCLE.md`):

1. Problem Definition (no coding) → 2. Business Requirements → 3. Architecture (coding prohibited until clear) → 4. Technical Design (coding begins only after approval) → 5. Implementation (small commits, always building) → 6. Testing → 7. Documentation → 8. Review (§25) → 9. Observability → 10. Release (§36).

No stage is skipped. No shortcuts for AI-generated work.

## 40. Standing Engineering Rules

Quick-reference summary — the rest of this document is the detail; this is the cheat sheet.

- **Motto:** "Engineering excellence over engineering speed."
- **The One Rule:** will this decision still look reasonable three years from now? If no, redesign.
- **The Golden Rule (modules):** can this module become an independent service without changing its business logic? If no, redesign.
- **The Golden Rule (domains):** which domain owns this? If more than one plausible answer exists, the design is probably wrong.
- Evidence before opinion. Architecture before implementation. Humans approve; AI never acts autonomously on production.
- `core` depends on nothing but the JDK. Immutable by default. Typed everything (IDs, results, errors). No `now()` calls outside `Clock`.
- Every public API is a long-term contract. Every architecture change needs an ADR. Every PR explains why, not just what.
- One remaining known gap: no automated architecture-boundary test yet enforcing §8 (still an open Track B item — see below).

---

**Housekeeping milestone (Aug 2026):** the Java-version inconsistency (§11), the project-identity inconsistency (§2, §29), and the non-functional Maven wrapper have all been resolved. The repository now consistently uses **Java 25** everywhere, **"Forge AI Platform"** as the product name and **"Forge Platform Core"** for the core-module-specific documents (kernel spec, core coding guidelines), and `./mvnw clean verify` builds successfully from a clean local Maven-wrapper cache. Package names (`io.forge.platform`) were intentionally left unchanged — no compelling technical reason existed to rename them, only the documentation-level naming was inconsistent.

**Still-open Track B items** (documentation/process only, none block Track A): an automated architecture-boundary test for §8's package dependency rules; the `AI_CONTEXT.md`-style continuity aid originally proposed and superseded by this document; migrating ADRs from the single `07_ARCHITECTURE_DECISIONS.md` file to a `docs/ADR/` directory per `ARCHITECTURE_STATUS.md`'s stated future plan; README's "Key documents" list is stale relative to `docs/INDEX.md` (missing `12`, `13`, `ARCHITECTURE_STATUS.md`); the `LICENSE` copyright line still reads "Forge AI" rather than "Forge AI Platform" (left untouched as a legal-document caution, not an oversight).
