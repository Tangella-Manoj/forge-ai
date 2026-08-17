package io.forge.platform.intelligence.model;

import io.forge.platform.core.validation.Validation;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The smallest useful representation of a repository as a system: every module's own structural
 * facts, plus the real (build-time) dependency relationships between them.
 *
 * <p>Deliberately not a general-purpose graph — no generic node/edge types, no persistence, no
 * query engine. Just two lists: what exists, and what depends on what. This is enough to answer the
 * concrete question that motivated it — "if module X changes, which other modules in this workspace
 * could be affected?" — without building infrastructure ahead of a use case that needs more.
 *
 * @param modules every module's own {@link RepositorySnapshot}, in scan order
 * @param moduleDependencies real, resolved inter-module dependencies within this workspace
 */
public record EngineeringModel(
    List<RepositorySnapshot> modules, Set<ModuleDependency> moduleDependencies) {

  public EngineeringModel {
    Validation.requireNonNull(modules, "modules must not be null");
    Validation.requireNonNull(moduleDependencies, "moduleDependencies must not be null");
    modules = List.copyOf(modules);
    moduleDependencies = Set.copyOf(moduleDependencies);
  }

  /**
   * Every module (by artifactId) that directly or transitively depends on {@code artifactId} within
   * this workspace — "if I change this module, what else in this workspace could be affected?"
   *
   * @param artifactId the module to find dependents of
   * @return the artifactIds of every module that depends on it, directly or transitively
   */
  public Set<String> dependentsOf(String artifactId) {
    Validation.requireNonBlank(artifactId, "artifactId must not be blank");

    Set<String> dependents = new LinkedHashSet<>();
    Deque<String> frontier = new ArrayDeque<>();
    frontier.add(artifactId);

    while (!frontier.isEmpty()) {
      String target = frontier.poll();
      for (ModuleDependency dependency : moduleDependencies) {
        if (dependency.toModule().equals(target) && dependents.add(dependency.fromModule())) {
          frontier.add(dependency.fromModule());
        }
      }
    }

    return Set.copyOf(dependents);
  }
}
