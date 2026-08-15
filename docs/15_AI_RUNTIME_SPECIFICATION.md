# Forge Platform Core
## AI Runtime Specification

**Version**

1.0 (draft — proposal stage, not yet approved for implementation)

## Purpose

Defines Sprint 3 (AI Runtime) before implementation, at the same level of detail the kernel spec gave Core and `14_PLATFORM_SERVICES_SPECIFICATION.md` gave Platform Services. Before this document, AI Runtime existed only as an eight-item capability name list (`CLAUDE.md` §4, `ARCHITECTURE_STATUS.md`) — no package structure, no sequencing, no acknowledgment of which pieces need real external credentials versus which are provider-agnostic design work.

**This document is a proposal.** No AI Runtime code should be written against it until reviewed.

## 0. Why AI Runtime is next, not Platform Services

`ADR-023` deferred Platform Services (Logging, Configuration, Validation, Serialization, Observability, Security Foundations) because none of them had a concrete caller — no log statements, no controllers, no custom config anywhere in the repository.

AI Runtime is the layer that creates that caller, by architecture, not by coincidence: `CLAUDE.md` §4 places `AI Runtime` directly above `Platform` and below `Engineering Intelligence`, and it is explicitly "the only layer permitted to know about a specific AI vendor." The moment this layer calls a real AI provider, it genuinely needs:

- **Configuration** — API keys, model names, timeouts, retry policy (typed, validated, not scattered `@Value` strings).
- **Logging** — request/response logging with mandatory secret redaction (API keys, and depending on product use, potentially sensitive prompt/response content).
- **Observability** — latency, token usage, error rate per provider call — this is the first genuinely externally-facing, failure-prone I/O in the entire codebase.
- **Security Foundations** — secret-handling conventions for API keys specifically.

This is not a decision to build Platform Services speculatively ahead of AI Runtime, nor a decision to build a throwaway web layer just to unblock it (both considered and rejected last session). It is proceeding to the next layer the documented architecture already calls for, which happens to resolve the Platform Services blocker as a side effect of real need — not the goal, but a coherent side effect worth noting for the record.

## 1. Package Structure

Recommended package root:

`io.forge.platform.ai` (sibling of `core` and the Platform Services packages, not nested under either)

Proposed subpackages, one per capability from the existing name list:

- `provider` — provider abstraction (the one every other subpackage depends on)
- `router` — model routing/selection
- `prompt` — prompt engine
- `context` — context engine
- `tool` — tool calling
- `mcp` — MCP integration
- `memory` — conversation/session memory
- `evaluation` — output evaluation
- `guardrail` — guardrails

### Why

Same reasoning as Platform Services (ADR-022): `core` stays untouched and framework-free; `ai` sits alongside `core` and the Platform packages as its own top-level concern, avoiding both a `core.ai` violation (Core must never know about AI, per its own definition) and an awkward `platform.ai` nesting (AI Runtime is architecturally a peer of Platform, not a child of it, per `CLAUDE.md` §4's layer diagram).

### Alternatives considered

- Nest under `core`: rejected outright — Core's own definition explicitly excludes AI dependencies (`CLAUDE.md` §4: "No AI dependencies" is listed as a Core constraint, same sentence as "no Spring dependency").
- Nest under the Platform packages (`platform.ai.*`): rejected — the architecture diagram places AI Runtime as its own layer between Platform and Engineering Intelligence, not inside Platform.
- One flat `ai` package: rejected for the same dumping-ground reason ADR-022 rejected it for Platform Services — nine unrelated capabilities in one package defeats ownership clarity.

### Trade-offs

Nine named-but-empty packages, same trade-off already accepted twice (Core's eight, Platform's six) — pre-declared structure over ad hoc placement, cost is zero until populated.

## 2. Dependency Rules

- `ai.*` may depend on `core` and any `platform.*` package (once those exist). Never the reverse.
- `ai.provider` is the foundational subpackage — every other `ai.*` subpackage should depend on it (to make an actual model call) rather than each duplicating provider-specific logic. `provider` itself depends on nothing else in `ai.*`.
- No `ai.*` package outside `provider` may import a vendor SDK directly (Anthropic's, OpenAI's, or any other). Only `provider`'s concrete implementations may do that — this is the literal meaning of "AI Runtime is the only layer permitted to know about a specific AI vendor" applied one level deeper: even within AI Runtime, only one subpackage should.

## 3. What can be designed and built now, versus what is blocked on a real decision

This is the most important split in this document, and the reason implementation stops after `provider`'s interface rather than continuing through all nine capabilities:

**Buildable without external credentials or a provider decision** (mirrors how `Clock` needed no real wall-clock library — just an interface and a fake):
- `ai.provider`'s **interface** — a vendor-agnostic contract for "send a prompt, get a completion," modeled the same way `Clock` abstracts "get the time": an interface plus a deterministic fake/stub implementation usable in tests. No API key required to design or test this.

**Blocked on a real decision that is not mine to make unilaterally:**
- Which AI provider(s) to actually integrate first (Anthropic, OpenAI, both via the abstraction, a local model) — a genuine "two materially different directions" choice with cost, licensing, and product-direction implications.
- The actual API key/credential to call any real provider — a hard stop per this project's own rules ("credentials/authorization unavailable to you are required").
- Everything past `provider`'s interface (`router`, `prompt`, `context`, `tool`, `mcp`, `memory`, `evaluation`, `guardrail`) depends on having at least one real provider integration to validate against — building them against zero real providers would repeat exactly the "no concrete caller" mistake ADR-023 just corrected for Platform Services. They are scoped here (§4) but explicitly **not** sequenced for implementation yet.

## 4. Capability scope (for when each is unblocked)

| Capability | Solves | Blocked on |
|---|---|---|
| `provider` | Vendor-agnostic "call a model" contract | Nothing, for the interface. Concrete implementations blocked on provider choice + API key. |
| `router` | Choosing which model/provider handles a given request | A second real provider to route between — one provider makes routing meaningless |
| `prompt` | Reusable, versioned prompt construction | A real feature that constructs prompts |
| `context` | Assembling context (repo data, history) into a request | Repository Intelligence / Knowledge Engine (later sprints) existing as a context source |
| `tool` | Tool-calling / function-calling support | A concrete tool a model would call — none exists yet |
| `mcp` | Model Context Protocol integration | A concrete MCP use case |
| `memory` | Conversation/session memory | A concrete multi-turn feature |
| `evaluation` | Scoring/evaluating AI outputs | Real outputs to evaluate |
| `guardrail` | Input/output safety filtering | A real provider call to guard |

Every row past `provider` is blocked on something this document cannot resolve alone — either a product feature that doesn't exist yet, or a later-sprint module. This table exists so each is picked up deliberately later, not forgotten and not built speculatively now.

## 5. What this document does not do

It does not add any AI provider SDK as a dependency. It does not choose a provider. It does not request or assume any API key. It does not implement `router` through `guardrail`. It proposes only: package structure, dependency rules, and the `provider` interface as the one piece of this layer with zero external blocker.
