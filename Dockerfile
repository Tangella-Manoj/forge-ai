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

# Only used by the web-server path (no CLI args). CLI invocations
# (`docker run <image> scan <path>`) ignore this and exit as normal.
EXPOSE 8080

# Uses the same actuator health endpoint deployment probes use — verifies the
# application layer is actually serving, not just that the process exists.
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -q -O /dev/null "http://localhost:${PORT:-8080}/actuator/health" || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
