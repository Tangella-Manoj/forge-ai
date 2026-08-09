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
