package io.forge.platform.intelligence.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import io.forge.platform.intelligence.repository.PackageDependency;
import io.forge.platform.intelligence.repository.RepositoryScanner;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CycleDetectorTest {

  /**
   * Dogfooding, and a real architectural claim about this repository: Forge, scanning itself,
   * proves its own package graph is acyclic — exactly what {@code ArchitectureTest}'s {@code
   * topLevelPackagesAreFreeOfCycles} rule already asserts at the top-level slice granularity, now
   * verified independently at full package granularity by production code, not a test-only tool.
   */
  @Test
  void thisRepositoryHasNoPackageCycles() {
    Result<RepositorySnapshot, PlatformError> result =
        RepositoryScanner.scan(Path.of(System.getProperty("user.dir")));
    assertTrue(result.isSuccess());
    RepositorySnapshot snapshot = result.fold(value -> value, error -> null);

    Set<CyclicPackageGroup> cycles = CycleDetector.findCycles(snapshot.internalDependencies());

    assertTrue(cycles.isEmpty(), () -> "expected no cycles, found: " + cycles);
  }

  @Test
  void findsNoCyclesInAnEmptyGraph() {
    assertTrue(CycleDetector.findCycles(Set.of()).isEmpty());
  }

  @Test
  void findsNoCyclesInALinearChain() {
    Set<PackageDependency> dependencies =
        Set.of(new PackageDependency("a", "b"), new PackageDependency("b", "c"));

    assertTrue(CycleDetector.findCycles(dependencies).isEmpty());
  }

  @Test
  void findsNoCyclesInADiamond() {
    Set<PackageDependency> dependencies =
        Set.of(
            new PackageDependency("a", "b"),
            new PackageDependency("a", "c"),
            new PackageDependency("b", "d"),
            new PackageDependency("c", "d"));

    assertTrue(CycleDetector.findCycles(dependencies).isEmpty());
  }

  @Test
  void detectsATwoPackageCycle() {
    Set<PackageDependency> dependencies =
        Set.of(new PackageDependency("a", "b"), new PackageDependency("b", "a"));

    Set<CyclicPackageGroup> cycles = CycleDetector.findCycles(dependencies);

    assertEquals(Set.of(new CyclicPackageGroup(Set.of("a", "b"))), cycles);
  }

  @Test
  void detectsAThreePackageCycle() {
    Set<PackageDependency> dependencies =
        Set.of(
            new PackageDependency("a", "b"),
            new PackageDependency("b", "c"),
            new PackageDependency("c", "a"));

    Set<CyclicPackageGroup> cycles = CycleDetector.findCycles(dependencies);

    assertEquals(Set.of(new CyclicPackageGroup(Set.of("a", "b", "c"))), cycles);
  }

  @Test
  void reportsOnlyTheCyclicComponentWhenMixedWithAcyclicPackages() {
    Set<PackageDependency> dependencies =
        Set.of(
            new PackageDependency("a", "b"),
            new PackageDependency("b", "a"),
            new PackageDependency("x", "y"));

    Set<CyclicPackageGroup> cycles = CycleDetector.findCycles(dependencies);

    assertEquals(Set.of(new CyclicPackageGroup(Set.of("a", "b"))), cycles);
  }

  @Test
  void detectsMultipleIndependentCycles() {
    Set<PackageDependency> dependencies =
        Set.of(
            new PackageDependency("a", "b"),
            new PackageDependency("b", "a"),
            new PackageDependency("x", "y"),
            new PackageDependency("y", "x"));

    Set<CyclicPackageGroup> cycles = CycleDetector.findCycles(dependencies);

    assertEquals(
        Set.of(new CyclicPackageGroup(Set.of("a", "b")), new CyclicPackageGroup(Set.of("x", "y"))),
        cycles);
  }

  @Test
  void rejectsNullDependencies() {
    assertThrows(NullPointerException.class, () -> CycleDetector.findCycles(null));
  }
}
