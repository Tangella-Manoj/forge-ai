# syntax=docker/dockerfile:1
#
# Multi-stage build: compile with the full JDK, run on a minimal JRE. Keeps the
# shipped image free of build tooling (Maven, source, test classes) — smaller
# attack surface and smaller image, not just a smaller download.

# ---- Build stage -----------------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /build

# Dependencies first, source second: a source-only change re-uses the
# dependency layer instead of re-downloading the whole repository.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ---- Runtime stage ----------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

# Runs as a dedicated non-root user — never as root in the container.
RUN addgroup -S forge && adduser -S forge -G forge
USER forge

WORKDIR /app
COPY --from=build --chown=forge:forge /build/target/forge-ai-*.jar app.jar

# A deployed container has no repository to analyze — there is no source checkout in it and the
# free-tier host has no persistent disk to mount one from. Without a workspace, every analysis
# endpoint correctly but uselessly returns 400. Shipping this project's own sources gives the
# running service something real to analyze (itself), which is the same dogfooding the test suite
# already does. Sources only, no build output: ~220KB.
COPY --from=build --chown=forge:forge /build/pom.xml /workspace/forge-ai/pom.xml
COPY --from=build --chown=forge:forge /build/src/main /workspace/forge-ai/src/main
ENV FORGE_WORKSPACE_ROOT=/workspace/forge-ai

# Only used by the web-server path (no CLI args). CLI invocations
# (`docker run <image> scan <path>`) ignore this and exit as normal.
EXPOSE 8080

# Uses the same actuator health endpoint deployment probes use — verifies the
# application layer is actually serving, not just that the process exists.
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -q -O /dev/null "http://localhost:${PORT:-8080}/actuator/health" || exit 1

# MaxRAMPercentage rather than a fixed -Xmx: the JVM's container default caps the heap at 25% of
# the cgroup limit, which wastes most of a small (512MB-class) instance. 75% leaves headroom for
# metaspace, code cache, and thread stacks.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
