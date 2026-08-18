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

## Running It

Build first: `./mvnw clean package`

**As a CLI** — analyze any local Maven project (single module or multi-module root):

```
java -jar target/forge-ai-0.1.0-SNAPSHOT.jar scan [path]
java -jar target/forge-ai-0.1.0-SNAPSHOT.jar impact <module> [path]
```

`scan` reports structure, packages, dependencies, circular dependencies, and risk findings.
`impact` reports what else a change to `<module>` would affect.

**As an API** — run with no arguments to start the web server instead:

```
java -jar target/forge-ai-0.1.0-SNAPSHOT.jar
```

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/analysis?repository=<path>` | Modules, module dependencies, and risk findings |
| `GET /api/v1/impact?module=<artifactId>&repository=<path>` | What a change to that module affects |
| `GET /actuator/health` | Liveness/readiness, for deployment probes |

`repository` is optional and always **relative to `FORGE_WORKSPACE_ROOT`** (defaults to the working
directory). Absolute paths and any path escaping that root are rejected — the API will only ever
read inside its configured workspace. Errors are [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)
Problem Details carrying a stable machine-readable `code`.

**In Docker:**

```
docker build -t forge-ai .
docker run -p 8080:8080 forge-ai
```

The image ships this project's own sources as its default workspace, so a fresh container has
something real to analyze — `curl localhost:8080/api/v1/analysis` returns Forge analyzing itself.
Mount another repository to analyze that instead:

```
docker run -p 8080:8080 -e FORGE_WORKSPACE_ROOT=/workspace \
  -v /path/to/your/project:/workspace/your-project:ro forge-ai
```

`render.yaml` deploys the same image to [Render](https://render.com) as a web service.

## Development Lifecycle

Before changing code, read:
- docs/08_ENGINEERING_DEVELOPMENT_LIFECYCLE.md
- docs/06_ENGINEERING_STANDARDS.md
- .github/copilot-instructions.md

## Contributing

See CONTRIBUTING.md for contribution guidelines, CODE_OF_CONDUCT.md for expected behaviour, and SECURITY.md for reporting vulnerabilities.

## License

This project is licensed under the terms in LICENSE.
