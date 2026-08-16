# Copilot Instructions — Forge AI Platform

Note: This file guides GitHub Copilot and other repository-scoped assistants. Keep concise; refer to docs/ for full context. See also CLAUDE.md at the repository root for the full engineering constitution.

Project vision
- Forge AI Platform (OpenEIOS): an Engineering Intelligence Platform. Focus on producing explainable, evidence-backed engineering decisions, not just code generation. (See docs/01_PROJECT_VISION.md)

Mission
- Reduce engineer time spent understanding, debugging, reviewing, documenting, and maintaining software while preserving quality, security, and reliability. (See docs/02_PRODUCT_REQUIREMENTS.md)

Architecture philosophy
- Modular Monolith → Modular Services
- DDD, Clean Architecture, Vertical Slice, Event-Driven
- Capabilities-first: Reasoning is a capability; models are implementations. (See docs/03_SYSTEM_ARCHITECTURE.md, docs/04_DOMAIN_MODEL.md)

Engineering principles (summary)
- Principle Zero: Optimize for better engineering decisions, not automation.
- Evidence before automation; Architecture before implementation; Humans accountable. (See docs/11_ENGINEERING_PRINCIPLES.md)

Coding standards (summary)
- Java 25, Maven, Spring Boot.
- SOLID, composition over inheritance, single responsibility per file.
- No field injection; constructor injection for Spring.
- Use DTOs for external APIs; never expose entities.
- Follow Conventional Commits.
- Tests required for all behavior.
(See docs/06_ENGINEERING_STANDARDS.md)

Base package
- io.forge.platform

Technology stack
- Current, actually in the repository: Java 25, Maven, Spring Boot, ArchUnit (test-scope).
- Long-term direction, not yet present (docs/09_TECHNICAL_STRATEGY.md): PostgreSQL, Redis, Kafka, Docker, Testcontainers, OpenTelemetry, Prometheus, Grafana. Do not assume any of these exist until you've checked pom.xml — this list is a plan, not current reality.

Repository structure
- Current, actual: a single Maven module (`src/main/java`, `src/test/java`), plus `docs/`, `.github/`, `scripts/` at the root. This is expected and correct for the project's current stage — not a violation of anything.
- Target future structure (docs/05_REPOSITORY_BLUEPRINT.md), not yet grown into: docs/ platform/ applications/ services/ sdk/ agents/ prompts/ workflows/ infrastructure/ scripts/ testing/ examples/ assets/ tools/ .github/

Naming conventions
- Use full, descriptive names. Avoid abbreviations, e.g., Identity Service, Repository Service.
- Branches: feat/<scope>-short-description
- Commits: Conventional Commits format.

Definition of Done (summary)
- Requirements implemented, tests passing, documentation updated, API documented, security reviewed, logging/metrics added, performance considered, code reviewed.
(See docs/06_ENGINEERING_STANDARDS.md and docs/08_ENGINEERING_DEVELOPMENT_LIFECYCLE.md)

AI coding rules (CLAUDE.md is authoritative if this summary and it ever disagree)
- AI may generate and commit code directly, including production code — this repository's actual practice, per CLAUDE.md §31. The gate is review and understanding before it's trusted, not a prohibition on AI writing it in the first place.
- Generated code must be reviewed, tested, and understood by a human before being trusted (CLAUDE.md ADR-018) — code existing is not the same as code being trustworthy.
- AI must reference docs/ (and CLAUDE.md) before generating code.
- AI must never modify production systems autonomously, silently resolve genuine architectural ambiguity by guessing, or treat frozen architecture as open for redesign without a stated reason and an ADR (CLAUDE.md §32).

Review process
- Small PRs; architecture reviewed before implementation for cross-cutting changes.
- PRs must include EDL checklist (see .github/PULL_REQUEST_TEMPLATE.md)

Security requirements
- Never commit credentials.
- Validate and sanitize all inputs.
- Follow SECURITY.md for responsible disclosure.

Testing requirements
- Unit, integration, contract tests where applicable.
  - Use Testcontainers for integration tests against external dependencies.

Docs reference
- Keep this file short; link to docs/ for full policies and ADRs. Use docs/INDEX.md as next-hop for readers.
