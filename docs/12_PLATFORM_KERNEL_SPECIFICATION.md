# Forge Platform Core
## Platform Kernel Specification

**Version**

1.0

## Purpose

This document defines the platform kernel before implementation.

The kernel is the lowest-level shared foundation of Forge Platform. Every future module depends on it, so the design must be explicit before code exists.

## 1. Package Structure

Recommended package root:

`io.forge.platform.core`

Subpackages:

- `id`
- `result`
- `error`
- `event`
- `time`
- `valueobject`
- `version`

### Why

This is the platform foundation, not a runtime kernel. `core` better expresses the stable base that every other module depends on.

### Alternatives considered

- A flat kernel package: simpler at first, but becomes harder to navigate.
- Feature-based packages: wrong for shared primitives.

### Trade-offs

More packages up front, but clearer ownership and lower coupling.

### Long-term impact

Allows the platform to grow without turning shared primitives into a dumping ground.

### Why it fits Forge Platform

Forge Platform is capability-first; the core must remain capability-neutral and universally reusable.

## 2. ID Strategy

Use strongly typed IDs as immutable value objects.

Recommended direction:

- each domain gets its own ID type
- every ID wraps a UUIDv7 internally
- equality is by wrapped UUID value
- IDs are serializable and safe to pass across module boundaries

Example categories:

- `WorkspaceId`
- `RepositoryId`
- `DecisionId`
- `AnalysisId`

### Why

Strongly typed IDs prevent accidental cross-domain mixing.

### Alternatives considered

- Raw UUID everywhere: easy initially, error-prone at scale.
- ULID everywhere: better ordering, but still weakly typed unless wrapped.
- String IDs: flexible, but too loose for a core kernel.

### Trade-offs

Typed IDs require a small amount of boilerplate, but they eliminate a large class of bugs. UUIDv7 provides temporal ordering while remaining a standard identifier format.

### Long-term impact

Safer APIs, better refactoring, clearer intent, stronger domain boundaries.

### Why it fits Forge Platform

Forge Platform will have many modules and capabilities; ID safety matters more than convenience.

## 3. Result Pattern

Use a dedicated `Result<T, E>` model instead of relying on exceptions for normal business flow.

Recommended shape:

- success contains a value
- failure contains a typed error
- no ambiguous null-based outcomes

### Why

Explicit outcomes make platform behavior easier to reason about and test. A typed failure channel keeps error semantics explicit.

### Alternatives considered

- Exceptions everywhere: fine for exceptional failures, poor for expected domain outcomes.
- `Result<T>`: simpler, but loses compile-time specificity for failures.
- `Either<L, R>`: expressive, but less familiar for most Java engineers.

### Trade-offs

`Result<T, E>` introduces a platform convention, but it standardizes error handling across modules and preserves error type information.

### Long-term impact

Less hidden control flow, better tests, more predictable APIs.

### Why it fits Forge Platform

Decision systems must explain outcomes clearly; `Result<T, E>` supports that goal.

## 4. Error Hierarchy

Use a three-layer structured error taxonomy.

Recommended characteristics:

- `PlatformError`
- `DomainError`
- `InfrastructureError`

Each error should carry:

- stable error code
- human-readable message
- machine-readable details
- optional cause for diagnostics

### Why

Scattered exception types create inconsistency and make API behavior hard to normalize. Separate layers preserve ownership and make failures easier to classify.

### Alternatives considered

- `RuntimeException` subclasses everywhere: flexible, but chaotic.
- Single generic exception: simple, but too coarse.

### Trade-offs

Structured errors require discipline, but they make APIs, logs, and tests consistent.

### Long-term impact

Better observability, better HTTP mapping later, easier automation.

### Why it fits Forge Platform

The platform must explain failures as clearly as it explains recommendations.

## 5. Event Model

Use a kernel-level `DomainEvent` abstraction with common metadata.

Recommended separation:

- `DomainEvent` for business meaning
- `IntegrationEvent` later, if needed, for boundary-crossing transport

Required metadata for every domain event:

- `EventId`
- `OccurredAt`
- `AggregateId`
- `Version`

### Why

The kernel should define the business event contract without binding itself to Kafka, Spring, or transport details.

### Alternatives considered

- One event type for everything: simpler, but blurs responsibilities.
- Spring Application Events: framework-bound and not portable enough.

### Trade-offs

The event contract becomes more explicit, but this enables deterministic tracking, auditing, and future event sourcing.

### Long-term impact

Keeps events portable and architecture-neutral.

### Why it fits Forge Platform

Events are a platform concern, not a framework concern.

## 6. Time Abstraction

Use an injected clock abstraction rather than direct time calls.

Recommended rule:

- no direct `now()` calls in core logic
- all time-sensitive logic depends on the kernel clock interface

### Why

Tests become deterministic and time behavior becomes explicit.

### Alternatives considered

- Direct `LocalDateTime.now()`: easy, but non-deterministic.
- Static time utilities: convenient, but hard to test.

### Trade-offs

Slightly more plumbing, much better testability.

### Long-term impact

Predictable behavior in tests, schedules, and audit scenarios.

### Why it fits Forge Platform

The platform will reason about decisions over time; time must be controllable.

## 7. Value Object Strategy

Prefer immutable value objects.

Recommended direction:

- small, immutable objects for validated concepts
- records should be preferred when they fit the domain semantics
- shared value-object base type only if it adds real consistency

### Why

Value objects make important domain concepts explicit and safe.

### Alternatives considered

- Plain primitives: too weak semantically.
- One abstract base class for everything: can become over-engineered.

### Trade-offs

Records are simple; a base abstraction can help consistency but should not force inheritance where composition is better.

### Long-term impact

Safer APIs, clearer modeling, better reuse.

### Why it fits Forge Platform

Forge Platform will express many engineering concepts that deserve first-class types.

## 8. Validation Strategy

Use Jakarta Validation for boundary/input validation and kernel-level validation primitives for shared rules.

Fail fast on invalid state.

Recommended split:

- Jakarta Validation: external input, DTOs, request boundaries
- Kernel validation: shared invariant checks for IDs, value objects, and platform primitives

### Why

Boundary validation and domain validation are not the same problem.

### Alternatives considered

- Only Jakarta Validation: insufficient for some kernel invariants.
- Custom validation everywhere: too much reinvention.

### Trade-offs

Two layers of validation, but each has a clear scope.

### Long-term impact

Cleaner boundaries, less duplicated logic, more explicit invariants.

### Why it fits Forge Platform

The kernel must stay reusable across modules, adapters, and eventually multiple products.

## 9. Serialization Strategy

Prefer framework-neutral kernel types that can be serialized by standard Java tooling and later adapted for JSON, OpenAPI, or other transports.

Recommended principle:

- serialization is an adapter concern
- kernel types remain transport-agnostic
- introduce serialization annotations only when a concrete adapter requires them

### Why

The kernel should not be coupled to any one transport format.

### Alternatives considered

- Jackson annotations everywhere: convenient, but transport leakage into the kernel.
- Protobuf-first: powerful, but unnecessary for Sprint 1.

### Trade-offs

Slightly more adapter work later, much better portability now.

### Long-term impact

Easier migration across REST, events, SDKs, and future protocols.

### Why it fits Forge Platform

Forge Platform is a platform, not a single API endpoint.

## 10. Thread Safety Principles

The kernel must be thread-safe by design.

Recommended rules:

- immutable by default
- no shared mutable state in kernel primitives
- no hidden caches unless explicitly designed and documented
- side-effect-free helpers where possible

### Why

Kernel primitives will be used everywhere, including concurrent request paths.

### Alternatives considered

- Allow mutable convenience objects: faster to write, harder to trust.

### Trade-offs

Immutability can create more object creation, but it dramatically reduces concurrency risk.

### Long-term impact

Safer concurrency, fewer heisenbugs, more predictable behavior.

### Why it fits Forge Platform

The kernel must remain stable under load and across modules.

## 11. Equality Rules

Equality must be explicit and type-specific.

Recommended rules:

- IDs compare by wrapped UUID value
- Value objects compare by contained values
- Entities compare by identity only

### Why

Equality bugs are a common source of domain defects and collection issues.

### Alternatives considered

- Default object identity everywhere: incorrect for value objects and IDs.
- Universal field-based equality: too broad for entities and mutable types.

### Trade-offs

Different equality semantics require discipline, but they make domain behavior correct and predictable.

### Long-term impact

Safer collections, cleaner tests, better refactoring behavior.

### Why it fits Forge Platform

Forge Platform needs consistent semantics across many modules and shared primitives.

## 12. Versioning and Compatibility

The Platform Core must follow semantic versioning.

Recommended rules:

- major version for breaking API changes
- minor version for backward-compatible additions
- patch version for backward-compatible fixes

Backward compatibility policy:

- do not remove public core APIs without deprecation
- deprecate first, remove later with a major version change
- preserve existing behavior unless a deliberate breaking change is documented

### Why

The core will be depended on by many modules; compatibility must be managed deliberately.

### Alternatives considered

- No formal versioning: too risky for a shared foundation.

### Trade-offs

Versioning adds process, but it protects downstream modules from unexpected breakage.

### Long-term impact

Stable evolution, safer upgrades, clearer release management.

### Why it fits Forge Platform

Forge Platform is intended to grow over years, not weeks.
## Summary Decision

The kernel should be:

- strongly typed
- immutable where practical
- framework-neutral
- explicit about outcomes
- deterministic in time-sensitive behavior
- safe for concurrent use
- semver-governed with compatibility discipline
- explicit about equality semantics

This specification exists so the kernel can be implemented once and trusted for years.
