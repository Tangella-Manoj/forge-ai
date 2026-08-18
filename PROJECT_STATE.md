# PROJECT_STATE.md — Forge AI Platform

**Purpose:** Live, current-state companion to `CLAUDE.md` (which is the stable constitution). This file tracks what's actually built, right now, so it doesn't need to be reconstructed from conversation history.

**Last updated:** Sprint 1 (Platform Core) complete. `ai.provider` (ADR-024), `intelligence.repository` (ADR-025), `intelligence.architecture` (ADR-026), `cli` (ADR-027), multi-module scanning (ADR-028), `reasoning` (ADR-029), `intelligence.model` (ADR-030), `intelligence.change` (ADR-031), `intelligence.risk` (ADR-032) all implemented. New: `api` (ADR-033) — the first product surface. `GET /api/v1/analysis` and `GET /api/v1/impact` expose scan/engineering-model/change-impact/risk-findings over HTTP, behind a dedicated `WorkspacePathResolver` trust boundary (rejects absolute paths, rejects normalized-path traversal outside the configured `forge.workspace.root`, tested including a "traversal hidden behind an innocent prefix" case). Errors are RFC 9457 Problem Details carrying `PlatformError`'s stable `code`. Actuator exposes only `health` (`show-details: never`) — verified live, not just in tests, that `/actuator/env`/`beans`/`configprops` 404. Reasoning/AI endpoint deliberately NOT exposed yet — no real AI provider is configured, and serving the `fixed` test double as an endpoint response would fabricate AI output. Live-verified: started the packaged jar as an actual server and queried it over real HTTP against the real DLMP repository — got the identical 8 modules, 6 module dependencies, and the same 2 risk findings as the CLI/ADR-032 validation. Forge now runs the full chain over HTTP: scan → engineering model → change impact → risk findings → evidence-backed JSON. Also new: deployment preparation — a multi-stage `Dockerfile` (JDK 25 build / JRE 25 Alpine runtime, non-root user, container health check), actually built and run locally (not just written) with health/security checks verified against the live container, plus a `docker-build` CI job that builds and health-checks the image on every push/PR. `render.yaml` blueprint targets Render's free tier (user's chosen host); the image bundles this project's own sources so the deployed service has something real to analyze rather than 400-ing every endpoint, verified under a 512MB memory cap. Deployment config is complete and locally verified but **not yet live** — Render deploys from a connected Git repo, so it inherits the GitHub blocker below. GitHub: `gh` authenticated as `Manoj-Ez` (company account); user confirmed `Tangella-Manoj` (same as DLMP) should own this repo — its SSH key already works here, so only repo *creation* is blocked, not the push (re-checked, still 404). 248/248 tests passing.

---

## Sprint Status

Sprint 0 (Foundation): ✅ Done.

Sprint 1 (Platform Core): ✅ Done.

| # | Item | Status |
|---|---|---|
| 1 | `Result<T,E>` | ✅ Implemented, tested, merged |
| 2 | `PlatformError` (`DomainError`/`InfrastructureError`) | ✅ Implemented, tested, merged |
| 3 | Typed IDs (`WorkspaceId`, `RepositoryId`, `DecisionId`, `AnalysisId`, `EventId`) | ✅ Implemented, tested, merged |
| 4 | Clock | ✅ Implemented, tested, merged |
| 5 | Value Objects | ✅ Resolved by engineering decision — no new artifact required (see below); no code produced |
| 6 | Domain Events | ✅ Implemented, tested, merged |
| 7 | Validation | ✅ Implemented, tested, merged (ADR-021) |
| 8 | Core Tests (broader hardening pass) | ✅ Closed the one real gap found: `InternalUuidGenerator` had zero direct tests despite gatekeeping every typed ID's validity — added boundary tests (version-vs-variant isolated) and a concurrency test (10k virtual-thread-concurrent generations, zero collisions) |

## What Exists Right Now

```
src/main/java/io/forge/platform/
├── ForgePlatformApplication.java
└── core/
    ├── error/
    │   ├── PlatformError.java       (sealed interface)
    │   ├── DomainError.java
    │   └── InfrastructureError.java
    ├── event/
    │   └── DomainEvent.java         (interface: eventId, occurredAt, aggregateId, version)
    ├── id/
    │   ├── TypedId.java             (interface)
    │   ├── InternalUuidGenerator.java (package-private UUIDv7 strategy)
    │   ├── WorkspaceId.java
    │   ├── RepositoryId.java
    │   ├── DecisionId.java
    │   ├── AnalysisId.java
    │   └── EventId.java
    ├── result/
    │   └── Result.java              (sealed interface: Success/Failure)
    ├── time/
    │   ├── Clock.java                (@FunctionalInterface)
    │   ├── SystemClock.java          (package-private)
    │   └── FixedClock.java           (package-private)
    └── validation/
        └── Validation.java          (final utility: requireNonNull/requireTrue/requireNonBlank)

src/main/java/io/forge/platform/ai/
└── provider/
    ├── AiProvider.java              (@FunctionalInterface: complete(AiPrompt) -> Result<AiCompletion, PlatformError>;
    │                                  static fixed(...)/failing(...) test doubles — no vendor SDK, no API key)
    ├── AiPrompt.java
    └── AiCompletion.java

src/main/java/io/forge/platform/intelligence/
└── repository/
    ├── RepositoryScanner.java       (final: scan(Path) -> Result<RepositorySnapshot, PlatformError>;
    │                                  scanWorkspace(Path) -> Result<List<RepositorySnapshot>, ...>
    │                                  for a whole multi-module project (ADR-028); resolves Maven
    │                                  parent-inherited groupId/version/java.version; reads pom.xml
    │                                  + walks src/main/java + parses import lines, no AI, no
    │                                  compilation required)
    ├── RepositorySnapshot.java      (coordinates, javaVersion, packages, internalDependencies)
    ├── PackageSummary.java          (name, classCount)
    ├── PackageDependency.java       (fromPackage, toPackage — observed internal import facts only;
    │                                  external/JDK/framework imports excluded)
    └── BuildCoordinates.java        (groupId, artifactId, version)

src/main/java/io/forge/platform/intelligence/
└── architecture/
    ├── CycleDetector.java           (final: findCycles(Set<PackageDependency>) -> Set<CyclicPackageGroup>;
    │                                  Tarjan's SCC algorithm, depends on intelligence.repository
    │                                  for PackageDependency — one-directional, ArchUnit-enforced)
    └── CyclicPackageGroup.java      (packages — a finding, not merely an observed fact)

src/main/java/io/forge/platform/cli/
├── RepositoryIntelligenceCli.java   (package-private @Component CommandLineRunner, gated behind
│                                      an explicit "scan" first arg — see ADR-027 for why)
└── RepositoryIntelligenceReport.java (package-private, framework-free: generate(Path) -> Outcome;
                                        renders coordinates/java version/packages/dependencies/cycles)

src/main/java/io/forge/platform/intelligence/
└── model/
    ├── EngineeringModel.java        (modules + moduleDependencies; dependentsOf(artifactId)
    │                                  answers "if X changes, what else is affected?" — ADR-030)
    ├── EngineeringModelBuilder.java (resolves each module's raw declaredDependencyArtifactIds
    │                                  against the workspace's real module set)
    └── ModuleDependency.java        (fromModule -> toModule; build-time coupling only, and its
                                       Javadoc says so — HTTP/route relationships not claimed)

src/main/java/io/forge/platform/intelligence/
└── risk/
    ├── RiskAnalyzer.java           (final: analyze(EngineeringModel) -> List<RiskFinding>;
    │                                 3 transparent rules, no score, no ML — ADR-032)
    ├── RiskFinding.java            (category, severity, subject, evidence, reason,
    │                                 recommendation — evidence and advice never merged)
    ├── RiskCategory.java           (CIRCULAR_PACKAGE_DEPENDENCY, CIRCULAR_MODULE_DEPENDENCY,
    │                                 CHANGE_AMPLIFICATION — one value per implemented rule)
    └── RiskSeverity.java           (HIGH = objective defect, MEDIUM = cost signal)

src/main/java/io/forge/platform/intelligence/
└── change/
    ├── ChangeImpactAnalyzer.java    (final: analyze(EngineeringModel, module) ->
    │                                  Result<ChangeImpact, PlatformError> — ADR-031; unknown
    │                                  module is a DomainError, not a misleading empty result)
    └── ChangeImpact.java            (changedModule, directDependents, transitiveDependents —
                                       kept separate, not summed: different engineering weight)

src/main/java/io/forge/platform/reasoning/
├── RepositoryAssessor.java          (final: assess(RepositorySnapshot, AiProvider) ->
│                                      Result<ArchitectureAssessment, PlatformError> — ADR-029;
│                                      the "Reason" stage; not wired into the CLI yet, see blockers)
└── ArchitectureAssessment.java      (evidence: List<String>, narrative: String — kept separate
                                       so fact and AI inference are never confused)

src/main/java/io/forge/platform/api/
├── WorkspaceProperties.java         (@ConfigurationProperties("forge.workspace"); root defaults to
│                                      process working directory — ADR-033)
├── WorkspacePathResolver.java       (package-private @Component: the trust boundary — rejects
│                                      absolute paths, resolves+normalizes relative paths beneath
│                                      root, rejects any result that escapes root; relativize() for
│                                      responses)
├── AnalysisResponse.java            (API-owned DTO: modules, moduleDependencies, riskFindings —
│                                      deliberately distinct from internal domain records)
├── ImpactResponse.java              (API-owned DTO: changedModule, directDependents,
│                                      transitiveDependents, scope — states the build-time-only
│                                      coupling limit on the wire, not just in Javadoc)
└── AnalysisController.java          (@RestController "/api/v1"; GET /analysis, GET /impact; thin —
                                       delegates entirely to intelligence.*; errors are RFC 9457
                                       ProblemDetail carrying PlatformError's code)
```

Run it (all verified working end-to-end, not assumed — including exit codes: 0 on success, 1 on failure):
- `./mvnw clean package`, then:
  - `java -jar target/forge-ai-0.1.0-SNAPSHOT.jar scan [path]` — structure, packages, dependencies, cycles, and (multi-module only) inter-module dependencies.
  - `java -jar target/forge-ai-0.1.0-SNAPSHOT.jar impact <module> [path]` — what else in the workspace a change to `<module>` affects.
  - `java -jar target/forge-ai-0.1.0-SNAPSHOT.jar` (no CLI args) — starts the web server instead (`PORT`, default 8080; `FORGE_WORKSPACE_ROOT`, default process working directory). Verified live: `GET /api/v1/analysis?repository=<relative path>`, `GET /api/v1/impact?module=<artifactId>&repository=<relative path>`, `GET /actuator/health`.
- or during development: `./mvnw spring-boot:run -Dspring-boot.run.arguments=scan`
- `[path]` can be a single Maven module or a multi-module project root — the CLI always uses `scanWorkspace` and reports every module found.

248/248 tests passing (includes `ArchitectureTest`, which has no production-code counterpart to list above but enforces §8's `core` dependency rules — now also covering `core`/`intelligence`, `ai`/`intelligence`, `repository`/`architecture`, `repository`+`model`/`change`, `cli`, `reasoning`, and `api` boundaries, plus cycle-freedom within `intelligence.*`'s own subpackages). `mvn clean verify` and `./mvnw clean verify` both green (Java 25, spotless clean, JaCoCo: 98% instruction / 95% branch coverage overall; `api` package 96–100%). `.github/dependabot.yml` added — Maven + GitHub Actions dependency scanning, activates once the repo is pushed to GitHub, no API key required.

**Deployment preparation — Dockerfile, health checks, CI image validation:** multi-stage `Dockerfile` (Maven/JDK 25 build stage → minimal JRE 25 Alpine runtime stage, non-root `forge` user, container `HEALTHCHECK` against `/actuator/health`, `EXPOSE 8080`). Not just written — actually built and run locally: `docker build` succeeds (348MB final image); a running container reports `healthy`, serves `/actuator/health` (`UP`), and 404s `/actuator/env` — matching the same security boundary verified against the bare jar. Then validated with a real repository bind-mounted in (`-v .../distributed-loan-management-platform:/workspace/dlmp:ro`, `FORGE_WORKSPACE_ROOT=/workspace`): `GET /api/v1/analysis?repository=dlmp` from inside the container returned the identical 8 modules and the same 2 risk findings (HIGH `loan-service`, MEDIUM `common`) as every prior DLMP validation. (One container start took several minutes to schedule — traced to this machine already running DLMP's own 17-container Docker Compose stack, not a Dockerfile or bind-mount defect; a retry once the daemon had capacity started in 8 seconds.) `.github/workflows/ci.yml` gained a `docker-build` job (runs after `build`) that builds the image, starts it, polls health until it reports UP (or fails the job), and asserts `/actuator/env` stays 404 — so a future change that breaks the container or reopens the actuator surface fails CI, not a deployment. Deliberately build-only: no registry push, since no container registry or push credentials have been chosen yet — adding that now would be speculative infrastructure ahead of an actual decision.

**Render deployment configuration (target chosen by the user: Render free tier):** `render.yaml` blueprint — single Docker web service, `plan: free`, `region: singapore`, `healthCheckPath: /actuator/health` (the same health contract the container `HEALTHCHECK` and CI job use, not a third one). No database/cache/worker declared, because the application uses none. Render terminates TLS and issues the `*.onrender.com` certificate, so HTTPS needs no configuration; a custom domain is attached from the dashboard once one is chosen. **A real product problem surfaced and fixed here:** a deployed container has no repository to analyze — there is no checkout inside it, and Render's free tier has no persistent disk to mount one from — so every analysis endpoint would have correctly but uselessly returned 400 while the service reported healthy. The image now bundles this project's own sources (`pom.xml` + `src/main`, ~220KB, no build output) at `/workspace/forge-ai` with `FORGE_WORKSPACE_ROOT` pointed at it, so the deployed service analyzes *itself* — the same dogfooding the test suite already does. Verified by running the image under `--memory=512m` (matching the free tier): starts healthy, `GET /api/v1/analysis` with no parameters returns a real self-analysis (1 module, 16 packages, 43 internal dependencies, Java 25, zero risk findings — honest, and expected for a single cycle-free module), traversal still 400s, `/actuator/env` still 404s, and the JVM settles at **146MB of 512MB** with `-XX:MaxRAMPercentage=75` (the JVM's 25% container default would have wasted most of a small instance). A CI step asserts the bundled workspace stays analyzable, so this can't silently regress back into a healthy-but-useless deployment.

**Deployment is prepared but not yet live — blocked on the same GitHub blocker as everything else.** Render deploys from a connected Git repository, and `Tangella-Manoj/forge-ai` still does not exist (re-checked, 404). Once the repo exists and this is pushed: connect the repo in the Render dashboard, Render reads `render.yaml`, builds the Dockerfile, and serves it over HTTPS. Nothing further needs to be written — the configuration is complete and locally verified. Free-tier caveat worth knowing before relying on the URL: the instance spins down after 15 minutes without traffic and takes roughly a minute to wake on the next request.

**`api` (ADR-033) — first product surface:** `GET /api/v1/analysis` and `GET /api/v1/impact`, thin adapters over the existing `intelligence.*` pipeline — no new analysis logic, only a new way to reach it. The security-critical piece is `WorkspacePathResolver`, which finally resolves the gap flagged back in the `intelligence.repository` note (item 2 under "Cross-session self-review" below): `RepositoryScanner` trusting its `Path` argument completely was fine while every caller was a local test or CLI operator, and explicitly flagged as needing revisiting "before this method is ever reachable from a web endpoint." It now is reachable from a web endpoint, and the resolver closes that gap — absolute paths rejected outright, relative paths resolved and normalized beneath `forge.workspace.root`, any result that escapes the root rejected, verified against both plain traversal and traversal hidden behind an innocent-looking prefix. Actuator exposes only `health`; `env`/`beans`/`configprops` verified to 404 both in tests and against a live running server. Errors are RFC 9457 Problem Details carrying `PlatformError`'s stable code. The reasoning/AI endpoint is deliberately not exposed — no real provider is configured, and serving `AiProvider.fixed()`'s canned output as a live endpoint response would fabricate AI output, which the standing rule forbids. Live-verified end-to-end against real DLMP (not just the test workspace): identical facts to the CLI/ADR-032 validation — 8 modules, 6 module dependencies, and exactly the same 2 risk findings.

**`intelligence.risk` (ADR-032) — Risk Intelligence:** three transparent rules over existing evidence — circular package dependencies (HIGH), circular module dependencies (HIGH), and change amplification (MEDIUM). No weighted score, no composite index, no ML: a number like "risk 7.3" implies precision this analysis doesn't have and can't explain, whereas "these two packages import each other" is checkable by hand in thirty seconds. **Change amplification is deliberately capped at MEDIUM** — DLMP's `common` is depended on by all 6 services, which is the *point* of extracting a shared module, not a defect; flagging correct architecture as HIGH RISK is how analysis tools teach engineers to ignore them. A test pins both the cap and the wording. Validated on DLMP: exactly 2 findings, both hand-verified against the source (the `loan-service` cycle is real — `LoanCommandService` → `saga`, `LoanDisbursementTransaction` → `command`), and **zero false positives** on the other 6 modules.

**`intelligence.change` (ADR-031) — Change Intelligence:** answers "if I change module X, what else is affected?", splitting direct from transitive dependents (different engineering weight; summing them would discard the distinction). Exposed as `forge-ai impact <module> [path]`. Verified live against DLMP: changing `common` correctly reports all 6 services as direct dependents, and the report always states its own limit (build-time Maven coupling only — DLMP's `api-gateway` routes to services over HTTP with no Maven dependency, and the model does not pretend to know that). **A real self-inflicted bug fixed here:** the first version returned the `impact` usage message as a *failure*, which made `run(...)` call `System.exit(1)` — killing the surefire JVM running the CLI's own tests, mid-build. Fixed twice over: usage is now success, and more importantly `System.exit` was moved out of the command logic entirely (`execute(...)` returns a boolean; `run(...)` translates it), so no command path can kill its own harness or a future embedding server. Regression test pins the contract.

**`intelligence.model` (ADR-030) — the Engineering Model:** represents a repository *as a system* rather than a bag of independent modules. `EngineeringModel.dependentsOf(artifactId)` answers the concrete question that motivated it — "if module X changes, what else in this workspace could be affected?" — the direct prerequisite for Change Intelligence and Risk Intelligence. Deliberately not a generic graph, no persistence, no query engine. Validated against DLMP's real 8-module workspace: found all six real `service -> common` dependencies, correctly excluded external libraries and `common`'s own self-reference, cross-checked independently against DLMP's actual `pom.xml` files. Building it also surfaced and fixed a real latent bug — `getElementsByTagName` searches the whole document, so a `<dependencyManagement>` block (or a `<profile>`'s `<properties>`) could be silently misread as a module's own; all three affected lookups now resolve only `<project>`'s direct children.

**`reasoning` (ADR-029):** `RepositoryAssessor` is the first capability combining Engineering Intelligence facts with the AI Runtime — builds an evidence list from a `RepositorySnapshot` (via `CycleDetector`), sends it to a caller-supplied `AiProvider`, returns both. Fully tested with `AiProvider.fixed`/`failing` doubles, including a prompt-capturing test proving the actual prompt sent embeds the real evidence, not a placeholder. Self-review caught a real trap before it shipped: wiring this into the CLI's default output using the fixed test double would print the same canned string regardless of what was actually found, which would misrepresent it as genuine AI-generated insight. Left un-wired rather than shipping something dishonest — ready to connect the moment either a real provider exists, or a decision is made to wire it in now with explicit "no real AI configured" labeling.

**Clean-checkout defect found and fixed:** `mvnw`/`mvnw.cmd`/`.mvn/` were gitignored — a fresh clone had no Maven wrapper at all (verified by actually cloning to a scratch directory, not assumed). This would have broken CI on the first push and every README-documented onboarding step. Fixed: wrapper now tracked, re-verified via a second fresh clone that `./mvnw clean verify` succeeds standalone. CI (`ci.yml`) also given Maven dependency caching (`cache: maven` on `actions/setup-java`).

**Multi-module scanning + Maven parent inheritance (ADR-028):** validated end-to-end against DLMP — an independent, real, 8-module Spring Boot project on this machine (read-only; never modified). Correctly inherited `groupId`/`version`/`java.version` for every child module and found a genuine circular dependency in `loan-service` (`command` ↔ `saga`). This is the first evidence Forge's Engineering Intelligence foundation generalizes beyond its own repository.

**`intelligence.repository` (ADR-025):** the first Engineering Intelligence capability. `RepositoryScanner` reads one Maven module's `pom.xml` and `src/main/java` tree into a deterministic `RepositorySnapshot` — build coordinates, Java version, per-package class counts. No AI, no inference, no external dependency beyond the JDK's built-in XML parser. Its own test suite dogfoods it against this repository (asserts the real `pom.xml`'s coordinates, Java 25, and `core.result`'s package/class count) — if the scanner and the repository it's scanning ever disagree, that test fails immediately.

**Cross-session self-review (holistic pass, not scoped to one commit):** audited the full public API surface built so far together for the first time. Two real findings, both documented rather than silently fixed or ignored:
1. **Naming ambiguity**: `core.id.RepositoryId` and `intelligence.repository` name the same English word for two unrelated concepts (a future domain entity identifier vs. concrete filesystem scanning). Explained in `CLAUDE.md` §29 rather than left to confuse a future reader. No code change — linking them now would be speculative (no Workspace/registration concept exists yet to actually issue a `RepositoryId` for a scanned path).
2. **Security note, resolved (ADR-033)**: `RepositoryScanner.scan(Path)` has no depth or file-count limit and trusts its `Path` argument completely — flagged as needing revisiting "before this method is ever reachable from a web endpoint." It now is: `io.forge.platform.api.WorkspacePathResolver` sits in front of every HTTP-reachable call, rejecting absolute paths and any normalized path that escapes the configured `forge.workspace.root`, so no caller-supplied path reaches `RepositoryScanner` untrusted. Depth/file-count limits remain unaddressed — no evidence yet that they're needed (the workspace root is operator-configured, not attacker-controlled), noted here so it isn't forgotten if that assumption changes.

**"Verified, not assumed" audit (prompted by the jar-packaging bug found while building the CLI):** actually ran, rather than assumed correct, several long-standing claims:
- `application.yaml`'s `management.endpoints.web.exposure.include`/`info.app.*` config was dead: verified empirically (`java -jar ... ` with no args starts and exits with no port ever listening — no `spring-boot-starter-web`, so Actuator has no HTTP transport to expose over). Removed the dead config; kept the `actuator` dependency itself as justified forward-provisioning for health checks once a web layer exists (explicitly required by this project's own deployment definition).
- `scripts/verify-dev-environment.sh` (referenced in README's Getting Started, never actually run this engagement): ran it — works correctly.
- `.github/CODEOWNERS` referenced `/infra/` (doesn't exist) and `@manoj` (not the real GitHub handle — confirmed via DLMP's own remote, `Tangella-Manoj`). Fixed both.
- `.github/copilot-instructions.md` (whose entire purpose is guiding AI assistants) had a corrupted/duplicated sentence, presented the *future* target repository structure and a fully aspirational technology stack (PostgreSQL, Redis, Kafka, Docker, Prometheus, Grafana — none present) as if they were current fact, and stated "AI must not write production changes directly" — contradicting `CLAUDE.md` §31's actual, more nuanced policy (which this whole engagement has operated under). All fixed to match reality and defer to `CLAUDE.md` on disagreement.

**Flagged, not fixed — needs your decision, not mine to guess:** `SECURITY.md` and `CODE_OF_CONDUCT.md` both list contact addresses at `@forge-ai.local` — `.local` is a non-routable reserved pseudo-domain (RFC 6762); these emails would bounce. This matters specifically because `SECURITY.md`'s entire purpose is enabling private vulnerability disclosure. Not fixed here because guessing a real contact address risks misdirecting a genuine security report — worse than leaving it visibly broken. Needs a real address from you (e.g., something at a domain you actually control and monitor) before this repository is published.

## Engineering Decisions Recorded This Sprint

- **Value Objects (item 5):** kernel spec §7 makes a shared base type discretionary ("only if it adds real consistency"). The pattern (immutable records, constructor-validated) is already fully demonstrated by `TypedId` and `PlatformError`'s implementations. No concrete domain value object exists yet to build against (no Identity/Workspace module). Building a placeholder example would be a speculative abstraction. **Decision: item considered satisfied by existing pattern; no dedicated code produced.**
- **`DomainEvent.aggregateId()` is `TypedId`, not generic.** A generic `DomainEvent<A extends TypedId>` would give concrete future events a strongly-typed aggregate reference without casting, but no concrete event exists yet to validate that design against, and generifying later is a source-breaking change once real implementers exist. Chose the simpler non-generic contract now; **flagging as a conscious, revisit-if-needed trade-off**, not an oversight.
- **`DomainEvent.version()` is a primitive `long`**, not a dedicated typed value object. Kernel spec §5 only requires "Version" as metadata without further shape; introducing a new `EventVersion` type with no second consumer would be speculative. Revisit if event-sourcing/optimistic-concurrency needs grow.

## Current Blockers

1. **GitHub push**: `gh` is now authenticated — but as `Manoj-Ez`, a company account unrelated to this project, confirmed by the user. The user confirmed `Tangella-Manoj` (same account DLMP uses) should own this repository instead. That account's SSH key (`git@github-personal`, alias already in `~/.ssh/config`) is verified working on this machine (`ssh -T` succeeds) — so pushing itself is not blocked, only *creating* the repository is, since SSH push can't create a new repo and `gh auth login` for a second account requires an interactive flow. Waiting on the user to create the empty `Tangella-Manoj/forge-ai` repo (public, no README/license/gitignore) via the GitHub web UI; push is immediate once it exists — re-checked via `gh api repos/Tangella-Manoj/forge-ai` at the deployment-preparation milestone, still 404. **This now also blocks deployment:** Render builds from a connected Git repository, so the Render service cannot be created until this repo is pushed. Everything on the Forge side (`render.yaml`, Dockerfile, health checks, memory tuning) is finished and locally verified.
2. **AI provider implementation**: blocked on two decisions that are not mine to make unilaterally (ADR-024) — which provider(s) to integrate, and API credentials. `ai.provider`'s interface is deliberately provider-neutral so this blocker stays contained to one future implementation class. `reasoning.RepositoryAssessor` (ADR-029) is built and tested against this interface, ready to connect the moment a provider is chosen.
3. **CLI wiring for `reasoning`**: a product-presentation decision, not an engineering blocker — wire `RepositoryAssessor` into the CLI now with explicit "no real AI configured, using a placeholder" labeling, or wait for a real provider. Not yet decided.

Everything past `ai.provider`'s interface (`router`, `prompt`, `context`, `tool`, `mcp`, `memory`, `evaluation`, `guardrail`) remains blocked on having at least one real provider integration to validate against (`docs/15_..md` §4). Platform Services (ADR-023) remains deferred, expected to activate once a provider is chosen. Neither blocker affects `intelligence.*` (ADR-025) — that layer needs no AI and no push to keep growing.

## Known, Tracked Issues

- The `platform.*` half of the dependency rule (`io.forge.platform.{logging,config,...}` may depend on `core`, never the reverse) has no ArchUnit rule yet — none of those packages have any classes, so a rule would be untestable/vacuous. Add it alongside the first Platform Services capability's first class.
- `LICENSE` copyright line still reads "Forge AI" rather than "Forge AI Platform" (left untouched deliberately as a legal-document caution).
- Local development requires JDK 25; the default JDK on this machine was 21. Installed Temurin 25 to `~/jdks` (user-local, no sudo) to build. **Resolved** — README's Getting Started now states the JDK 25 prerequisite explicitly (verified while documenting the API/Docker usage; the item was already stale when re-checked).
- Forge deployed as a container analyzes exactly one repository: itself (ADR-034). Analyzing arbitrary repositories over the network needs a fetch capability (clone a Git URL) with its own security design — untrusted clone targets, disk consumption, hostile input parsed at scale. Deliberately not built; mounting a volume covers the local case today.
