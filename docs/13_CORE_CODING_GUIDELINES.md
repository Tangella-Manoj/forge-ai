# Forge Platform Core
## Core Coding Guidelines

**Version**

1.0

## Purpose

These guidelines define how the Platform Core is implemented.

They are narrower than architecture: architecture says what belongs in the core; these rules say how to code it.

## 1. Records vs Classes

- Prefer `record` for immutable value carriers with no identity and minimal behavior.
- Use `final class` when invariants, lazy computation, or custom methods justify it.
- Do not use mutable classes for core primitives.

## 2. Sealed Classes

- Use sealed classes for closed hierarchies such as errors and event/result variants.
- Prefer sealed hierarchies when the allowed subtypes are known and stable.
- Do not use sealed types for open extension points.

## 3. Interfaces

- Use interfaces for capabilities, ports, and adapter contracts.
- Keep interfaces small and intention-revealing.
- Do not create interfaces by default for single concrete implementations unless future substitution is likely.

## 4. Exception Usage Policy

- Use exceptions only for truly exceptional, unrecoverable, or infrastructure-level failures.
- Do not use exceptions for normal control flow.
- Core APIs should prefer `Result<T, E>` for expected failure paths.

## 5. `Result<T, E>` Implementation Guidelines

- Model success and failure explicitly.
- Keep the success and error types type-safe and non-null where practical.
- Provide clear factory methods for success and failure.
- Avoid nesting `Result` unless a higher-level API truly needs it.

## 6. UUIDv7 Usage Guidelines

- Every typed ID wraps UUIDv7 internally.
- Never expose raw UUIDs across public core APIs when a typed ID exists.
- UUIDv7 is the canonical identifier format for core entities and events unless a future ADR states otherwise.

## 7. Equality Guidelines

- IDs compare by wrapped UUID value.
- Value objects compare by contained values.
- Entities compare by identity only.
- Do not include derived or transient fields in equality for value objects.

## 8. Immutability Rules

- Core types are immutable by default.
- Prefer constructor-based initialization with all required state present.
- Never mutate IDs, errors, events, or value objects after creation.
- If mutation is required, model it as a new object or a domain transition.

## 9. Thread Safety Rules

- Core types must be safe for concurrent use.
- Avoid shared mutable state.
- Prefer stateless helpers and pure functions.
- If caching is required later, keep it outside the core types.

## 10. Package Dependency Rules

- `core` must depend on nothing outside Java standard library and minimal annotations only if unavoidable.
- `platform` may depend on `core`, but not the reverse.
- Modules depend on `core` and `platform`, never the other way around.
- Do not introduce framework dependencies into core packages.

## Default Review Check

Before merging core code, ask:

- Is this immutable by default?
- Does it preserve typed safety?
- Does it avoid framework coupling?
- Does it keep equality semantics correct?
- Does it keep failure handling explicit?

If the answer to any of these is no, redesign before merging.
