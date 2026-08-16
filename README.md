# Forge AI Platform

Engineering Intelligence Platform — OpenEIOS

Forge AI Platform is an open-source Engineering Intelligence Platform and the reference architecture for AI-native engineering systems. It helps teams make well-evidenced engineering decisions by combining repository analysis, observability, knowledge, and reasoning.

Status: Sprint 1 (Platform Core) complete. AI Runtime in progress — `ai.provider` interface implemented, provider-neutral, no concrete provider yet. Engineering Intelligence started ahead of schedule (evidence-based resequencing, see `CLAUDE.md` §29) — `intelligence.repository` and `intelligence.architecture` implemented. See `PROJECT_STATE.md` for the live, current picture.

## Architecture

- Modular Monolith → Modular Services
- Domain-Driven Design
- Clean Architecture
- Vertical Slice Architecture
- Event-Driven Architecture

## Documentation

Start here:
- docs/INDEX.md

Key documents:
- docs/01_PROJECT_VISION.md
- docs/02_PRODUCT_REQUIREMENTS.md
- docs/03_SYSTEM_ARCHITECTURE.md
- docs/04_DOMAIN_MODEL.md
- docs/05_REPOSITORY_BLUEPRINT.md
- docs/06_ENGINEERING_STANDARDS.md
- docs/07_ARCHITECTURE_DECISIONS.md
- docs/08_ENGINEERING_DEVELOPMENT_LIFECYCLE.md
- docs/09_TECHNICAL_STRATEGY.md
- docs/10_PLATFORM_CAPABILITY_MAP.md
- docs/11_ENGINEERING_PRINCIPLES.md
- docs/12_PLATFORM_KERNEL_SPECIFICATION.md
- docs/13_CORE_CODING_GUIDELINES.md
- docs/14_PLATFORM_SERVICES_SPECIFICATION.md
- docs/15_AI_RUNTIME_SPECIFICATION.md
- docs/ARCHITECTURE_STATUS.md
- PROJECT_STATE.md — live current state, more current than any document above

## Getting Started

**Prerequisite:** JDK 25 (matching `pom.xml` and CI). If your default JDK is older, install JDK 25 separately and point `JAVA_HOME` at it before building — the Maven wrapper does not install a JDK for you.

1. Clone:
   `git clone git@github.com:your-org/forge-ai.git`
2. Read the documentation index:
   `docs/INDEX.md`
3. Run the development environment check:
   `./scripts/verify-dev-environment.sh`

## Development Lifecycle

Before changing code, read:
- docs/08_ENGINEERING_DEVELOPMENT_LIFECYCLE.md
- docs/06_ENGINEERING_STANDARDS.md
- .github/copilot-instructions.md

## Contributing

See CONTRIBUTING.md for contribution guidelines, CODE_OF_CONDUCT.md for expected behaviour, and SECURITY.md for reporting vulnerabilities.

## License

This project is licensed under the terms in LICENSE.
