package io.forge.platform.intelligence.architecture;

import io.forge.platform.core.validation.Validation;
import io.forge.platform.intelligence.repository.PackageDependency;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds circular dependencies among packages, given the internal import facts a {@link
 * io.forge.platform.intelligence.repository.RepositoryScanner} observes.
 *
 * <p>Uses Tarjan's strongly-connected-components algorithm: every strongly connected component of
 * size two or more is, by definition, a set of packages each reachable from every other — a cycle.
 * A component of size one is not reported (that just means "not part of any cycle," not a
 * self-cycle — {@link PackageDependency} already forbids a package depending on itself).
 *
 * <p>Recursive implementation; fine for realistic repository package graphs (tens to low hundreds
 * of packages). Not intended for pathologically large graphs — revisit with an iterative
 * implementation if a real use case ever needs one.
 */
public final class CycleDetector {

  private CycleDetector() {}

  /**
   * Finds all groups of mutually-dependent packages within {@code dependencies}.
   *
   * @param dependencies the observed internal package dependencies to analyze
   * @return every cyclic group found; empty when the dependency graph is acyclic
   */
  public static Set<CyclicPackageGroup> findCycles(Set<PackageDependency> dependencies) {
    Validation.requireNonNull(dependencies, "dependencies must not be null");

    Map<String, List<String>> adjacency = new LinkedHashMap<>();
    for (PackageDependency dependency : dependencies) {
      adjacency
          .computeIfAbsent(dependency.fromPackage(), key -> new ArrayList<>())
          .add(dependency.toPackage());
      adjacency.putIfAbsent(dependency.toPackage(), new ArrayList<>());
    }

    TarjanScan scan = new TarjanScan(adjacency);
    for (String node : adjacency.keySet()) {
      if (!scan.indices.containsKey(node)) {
        scan.strongConnect(node);
      }
    }

    return scan.components.stream()
        .filter(component -> component.size() >= 2)
        .map(CyclicPackageGroup::new)
        .collect(Collectors.toUnmodifiableSet());
  }

  /** Tarjan's algorithm, one instance per {@link #findCycles} call. */
  private static final class TarjanScan {
    private final Map<String, List<String>> adjacency;
    private final Map<String, Integer> indices = new HashMap<>();
    private final Map<String, Integer> lowLinks = new HashMap<>();
    private final Deque<String> stack = new ArrayDeque<>();
    private final Set<String> onStack = new HashSet<>();
    private final List<Set<String>> components = new ArrayList<>();
    private int nextIndex = 0;

    TarjanScan(Map<String, List<String>> adjacency) {
      this.adjacency = adjacency;
    }

    void strongConnect(String node) {
      indices.put(node, nextIndex);
      lowLinks.put(node, nextIndex);
      nextIndex++;
      stack.push(node);
      onStack.add(node);

      for (String neighbor : adjacency.getOrDefault(node, List.of())) {
        if (!indices.containsKey(neighbor)) {
          strongConnect(neighbor);
          lowLinks.put(node, Math.min(lowLinks.get(node), lowLinks.get(neighbor)));
        } else if (onStack.contains(neighbor)) {
          lowLinks.put(node, Math.min(lowLinks.get(node), indices.get(neighbor)));
        }
      }

      if (lowLinks.get(node).equals(indices.get(node))) {
        Set<String> component = new LinkedHashSet<>();
        String member;
        do {
          member = stack.pop();
          onStack.remove(member);
          component.add(member);
        } while (!member.equals(node));
        components.add(component);
      }
    }
  }
}
