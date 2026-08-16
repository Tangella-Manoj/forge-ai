package io.forge.platform.intelligence.repository;

import io.forge.platform.core.validation.Validation;
import java.util.List;
import java.util.Set;

/**
 * A deterministic structural snapshot of one Java/Maven module: its build coordinates, declared
 * Java version, the packages found under its main source root, and the internal (within-module)
 * dependencies between those packages.
 *
 * <p>This is the smallest useful representation of "Repository Intelligence" (product mission:
 * "understand software repositories and engineering systems") — facts a scanner can observe
 * directly, with no inference and no AI involved. Richer analysis (symbol-level graphs,
 * architecture rule evaluation, multi-module aggregation) is deliberately deferred until a concrete
 * use case needs it, rather than built speculatively here.
 *
 * @param coordinates the module's build coordinates
 * @param javaVersion the module's declared Java release version
 * @param packages the packages found under the module's main source root, in scan order
 * @param internalDependencies observed package-to-package import relationships within this module
 */
public record RepositorySnapshot(
    BuildCoordinates coordinates,
    int javaVersion,
    List<PackageSummary> packages,
    Set<PackageDependency> internalDependencies) {

  public RepositorySnapshot {
    Validation.requireNonNull(coordinates, "coordinates must not be null");
    Validation.requireTrue(javaVersion > 0, "javaVersion must be positive");
    Validation.requireNonNull(packages, "packages must not be null");
    Validation.requireNonNull(internalDependencies, "internalDependencies must not be null");
    packages = List.copyOf(packages);
    internalDependencies = Set.copyOf(internalDependencies);
  }
}
