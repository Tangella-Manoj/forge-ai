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
- Java 25, Maven, Spring Boot, PostgreSQL, Redis, Kafka, Docker, Testcontainers, OpenTelemetry, Prometheus, Grafana.
(See docs/09_TECHNICAL_STRATEGY.md)

Repository structure
- docs/ platform/ applications/ services/ sdk/ agents/ prompts/ workflows/ infrastructure/ scripts/ testing/ examples/ assets/ tools/ .github/
(See docs/05_REPOSITORY_BLUEPRINT.md)

Naming conventions
- Use full, descriptive names. Avoid abbreviations. e- Use full, descriptive names. Avoid abbreviations. e.g., Identity Service, Repository Service.
  .g., Identity Service, Repository Service.
- Branches: feat/<scope>-short-description
- Commits: Conventional Commits format.

Definition of Done (summary)
- Requirements implemented, tests passing, documentation updated, API documented, security reviewed, logging/metrics added, performance considered, code reviewed.
(See docs/06_ENGINEERING_STANDARDS.md and docs/08_ENGINEERING_DEVELOPMENT_LIFECYCLE.md)

AI coding rules
- AI may suggest code, but generated code must be reviewed, tested, and understandable before merging.
- AI must not write production changes directly.
- AI must reference docs/ before generating code.

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
