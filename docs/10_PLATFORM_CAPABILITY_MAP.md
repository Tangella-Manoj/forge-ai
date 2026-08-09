# Forge AI Platform
## Platform Capability Map

**Version**

1.0

## Purpose

Define platform capabilities (not features). Map what belongs in Version 1 vs later and show dependencies so implementation aligns to capabilities.

## Capability Layers

### Layer 1 — Foundation

Permanent core capabilities:
- Identity
- Workspace
- Platform Kernel (configuration, events)
- Logging
- Events
- Storage
- Security
- Observability

### Layer 2 — Reasoning (Intelligence)

The brain of the platform; provider-agnostic:
- AI Reasoning / Prompt Engine
- Repository Understanding
- Knowledge
- Architecture Analysis
- Planning & Evaluation
- Context Builder
- Memory

### Layer 3 — Engineering

Core engineering capabilities consumed by users:
- PR Review
- Repository Review
- Performance Review
- Incident Analysis
- Documentation Generation
- Dependency Review
- Security Analysis
- Migration Assistant
- API / SQL Review

### Layer 4 — Automation

Execution and orchestration:
- Workflow Engine
- Agent Engine
- Scheduler
- Code/PR Generation
- Deployment & Pipelines
- Task Runner

### Layer 5 — Products

User-facing surfaces:
- Dashboard
- CLI
- VS Code extension
- REST API
- Enterprise Portal
- SDKs
- Mobile (future)

## Capability → Workflow → Agents → Tools → Providers

Design capabilities first. For each capability define:
- Workflows (what engineers expect)
- Agents (automated actors that execute workflows)
- Tools (APIs, parsers, connectors)
- Providers (LLMs, CI, cloud) as replaceable adapters

Example: Repository Intelligence
- Workflow: Analyze repository
- Agents: Architecture Agent, Security Agent, Review Agent
- Tools: GitHub API, Git, AST parsers, build tooling
- Providers: OpenAI, Anthropic, Local LLMs

## Final Capability Tree

Forge Platform
- Foundation
- Reasoning
- Knowledge
- Repository
- Architecture
- Performance
- Security
- Automation
- Analytics
- Products

## Principles

- Capabilities own behavior; providers are adapters.
- No "AI module"—reasoning is a capability; models are implementations.
- Design for vendor independence and clear ownership.

## Next

Use this map to prioritize Version 1 capabilities and to guide the upcoming "Forge Engineering Principles" (Step 11).