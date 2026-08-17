package io.forge.platform.intelligence.repository;

import io.forge.platform.core.validation.Validation;
import java.util.List;
import java.util.Set;

/**
 * A deterministic structural snapshot of one Java/Maven module: its build coordinates, declared
 * Java version, the packages found under its main source root, the internal (within-module)
 * dependencies between those packages, and the artifactIds this module's own {@code pom.xml}
 * declares as dependencies.
 *
 * <p>{@code declaredDependencyArtifactIds} is a raw, unresolved fact — every {@code <dependency>}
 * this module's {@code pom.xml} lists, whether it turns out to be another module in the same
 * workspace or an external library (Spring Boot starters, etc.). Resolving it against the other
 * modules in a workspace to find real inter-module relationships is {@code intelligence.model}'s
 * job, not this type's — the same "collect the raw fact now, resolve once everything is known"
 * split already used for {@link PackageDependency}.
 *
 * <p>This is the smallest useful representation of "Repository Intelligence" (product mission:
 * "understand software repositories and engineering systems") — facts a scanner can observe
 * directly, with no inference and no AI involved. Richer analysis (symbol-level graphs,
 * architecture rule evaluation) is deliberately deferred until a concrete use case needs it, rather
 * than built speculatively here.
 *
 * @param coordinates the module's build coordinates
 * @param javaVersion the module's declared Java release version
 * @param packages the packages found under the module's main source root, in scan order
 * @param internalDependencies observed package-to-package import relationships within this module
 * @param declaredDependencyArtifactIds every artifactId this module's own {@code pom.xml} declares
 *     a {@code <dependency>} on — unresolved, may include external libraries
 */
public record RepositorySnapshot(
    BuildCoordinates coordinates,
    int javaVersion,
    List<PackageSummary> packages,
    Set<PackageDependency> internalDependencies,
    Set<String> declaredDependencyArtifactIds) {

  public RepositorySnapshot {
    Validation.requireNonNull(coordinates, "coordinates must not be null");
    Validation.requireTrue(javaVersion > 0, "javaVersion must be positive");
    Validation.requireNonNull(packages, "packages must not be null");
    Validation.requireNonNull(internalDependencies, "internalDependencies must not be null");
    Validation.requireNonNull(
        declaredDependencyArtifactIds, "declaredDependencyArtifactIds must not be null");
    packages = List.copyOf(packages);
    internalDependencies = Set.copyOf(internalDependencies);
    declaredDependencyArtifactIds = Set.copyOf(declaredDependencyArtifactIds);
  }
}
