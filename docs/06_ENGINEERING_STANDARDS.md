# Forge AI Platform
## Engineering Constitution

**Version**

1.0

## Purpose

This document defines how Forge AI is built.

These rules override developer preferences.

Every pull request.

Every AI generated code.

Every feature.

Must follow these rules.

## Philosophy

We optimize for

Readability
Maintainability
Simplicity
Scalability
Reliability

NOT

Writing clever code.

## The Five Laws of Forge AI

### Law 1

Code is written for humans.

Computers only execute it.

Future engineers must understand it in minutes.

### Law 2

If code needs comments to explain what it does,

the design is probably wrong.

Comments should explain why,

not what.

### Law 3

Every class must have one responsibility.

If a class has "and" in its description,

split it.

### Law 4

Every public API

must be stable.

Breaking changes require

versioning.

### Law 5

Everything must be testable.

If something cannot be tested,

its design is wrong.

## Engineering Principles

SOLID

Mandatory.

Clean Architecture

Mandatory.

Domain Driven Design

Mandatory.

Vertical Slice Architecture

Mandatory.

Event Driven Architecture

Preferred

when asynchronous behaviour exists.

Composition

Preferred

over inheritance.

Immutable Objects

Preferred

whenever practical.

## Java Rules

Always use

Latest LTS Java

(Currently Java 25)

Never use

Legacy patterns

without justification.

Prefer

Records

Sealed Classes

Pattern Matching

Virtual Threads

Structured Concurrency

Modern Streams

## Spring Boot Rules

No Field Injection.

Always Constructor Injection.

Never

@Autowired

private UserService

Always

public UserController(UserService service)

Never expose entities directly.

Always DTOs.

Never

Business Logic

inside

Controller.

Never

Database Logic

inside

Controller.

Controllers only

Receive

Validate

Delegate

Respond

## Database Rules

Every table

must have

UUID Primary Key

Created At

Updated At

Version

Soft Delete

Audit Metadata

when appropriate.

No

SELECT *

Ever.

Indexes

must be intentional.

Every migration

must be reversible.

## API Rules

REST

must follow

OpenAPI.

Errors

must follow

RFC 9457 (Problem Details for HTTP APIs)

Never

return

{
"status":"failed"
}

Use structured error responses.

## Logging Rules

Never

System.out.println()

Ever.

Use structured logging.

Every log

must include

Request ID

Correlation ID

Timestamp

Service Name

## Exception Rules

Never catch Exception.

Catch specific exceptions.

Every exception

must have

Meaning

Context

Action

## Testing Rules

Every feature

must include

Unit Tests

Integration Tests

Contract Tests

where applicable.

Bug fixes

must include

Regression Tests.

## Security Rules

Never trust

Client Input.

Validate everything.

Escape everything.

Sanitize everything.

Never store

Secrets

Passwords

Tokens

inside code.

## AI Rules

AI

never

writes directly

to production.

AI

generates

recommendations.

Humans approve.

AI

must explain

its reasoning

where possible.

Every AI response

should include

Confidence

Evidence

Limitations

## Git Rules

Never commit

Broken Build

Temporary Code

Commented Code

Generated Files

Credentials

Every commit

must build successfully.

Commit Messages

Follow Conventional Commits.

Example

feat(identity): add JWT authentication

fix(repository): resolve cache invalidation bug

refactor(ai): simplify prompt builder

test(api): add integration tests

## Pull Request Rules

Every PR

must answer

Why?

What changed?

How tested?

Risk?

Rollback?

## Documentation Rule

Every public class

must have

purpose.

Every module

must have README.

Every architecture change

requires

ADR.

## Performance Rule

Measure.

Never guess.

Every optimization

must be backed

by

Evidence.

Benchmark.

Profiler.

Metrics.

## Code Review Rule

Review

Architecture

before

Syntax.

Correct architecture

beats

beautiful syntax.

## Future Rule

Whenever Java

or AI evolves,

prefer

better engineering,

not

newer trends.

## Definition of Done

A feature is complete only if:

Requirements implemented.

Tests passing.

Documentation updated.

API documented.

Security reviewed.

Logging added.

Metrics added.

Error handling completed.

Performance considered.

Code reviewed.

Build successful.

Otherwise,

it is not finished.

## Forge AI Motto

"Engineering excellence over engineering speed."

## Why this step matters

From now on, every time GitHub Copilot generates code, it should be measured against these standards—not just whether it compiles.

## What comes next (Step 7)

We are now ready to define the Architecture Decision Records (ADRs).

This is where we document why we choose Java over another language, why we start with a modular monolith, why we use PostgreSQL, Kafka, Redis, and other core technologies. These decisions become the long-term memory of the project, making it much easier for both humans and AI to understand the reasoning behind the architecture.

This is one of the practices that distinguishes mature engineering teams from most portfolio projects.
