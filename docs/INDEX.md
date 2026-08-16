# Documentation Index — Forge AI Platform

Start here. This index organizes the project's foundational documents and tells contributors when to read each.

Getting started (read first)
- README.md — project overview and current sprint status
- PROJECT_STATE.md — live current state: what's built, what's blocked, what's next (more current than any document below; read this before assuming a sprint/track status is still accurate)
- docs/01_PROJECT_VISION.md — high-level vision
- docs/02_PRODUCT_REQUIREMENTS.md — PRD
- docs/05_REPOSITORY_BLUEPRINT.md — where files live
- .github/copilot-instructions.md — AI assistant guidance

Architecture & design (read during design reviews)
- docs/03_SYSTEM_ARCHITECTURE.md — system blueprint
- docs/04_DOMAIN_MODEL.md — domain boundaries
- docs/10_PLATFORM_CAPABILITY_MAP.md — capability map
- docs/07_ARCHITECTURE_DECISIONS.md — ADRs (includes `intelligence.repository`/`intelligence.architecture`, ADR-025/026 — no dedicated spec doc; each is small enough that its ADR carries the full design rationale)
- docs/12_PLATFORM_KERNEL_SPECIFICATION.md — kernel design
- docs/14_PLATFORM_SERVICES_SPECIFICATION.md — Platform Services design (draft, proposal stage)
- docs/15_AI_RUNTIME_SPECIFICATION.md — AI Runtime design (`ai.provider` implemented per ADR-024; everything past its interface remains draft/blocked on a provider decision — see PROJECT_STATE.md)

Standards & policies (read before implementing code)
- docs/06_ENGINEERING_STANDARDS.md — coding/test/security standards
- docs/09_TECHNICAL_STRATEGY.md — tech selection framework
- docs/11_ENGINEERING_PRINCIPLES.md — decision-first principles
- docs/08_ENGINEERING_DEVELOPMENT_LIFECYCLE.md — feature lifecycle
- docs/13_CORE_CODING_GUIDELINES.md — core implementation standards
- docs/ARCHITECTURE_STATUS.md — architecture freeze status

Repository hygiene & contribution (read before contributing)
- CONTRIBUTING.md
- CODE_OF_CONDUCT.md
- SECURITY.md
- .github/ISSUE_TEMPLATE and .github/PULL_REQUEST_TEMPLATE.md

Operational & infra (read when working on CI/CD or infra)
- docs/05_REPOSITORY_BLUEPRINT.md
- docs/03_SYSTEM_ARCHITECTURE.md

When to read
- New contributors: start with Getting started section.
- Engineers preparing a design: read Architecture & design and Standards.
- Implementers: read Standards & EDL before coding.
- Maintainers: read ADRs and Technical Strategy.

Keep this index current. Add links to new docs as they are created.
