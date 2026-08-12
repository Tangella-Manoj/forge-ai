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
