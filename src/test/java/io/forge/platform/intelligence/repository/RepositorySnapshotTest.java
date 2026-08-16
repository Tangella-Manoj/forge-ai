package io.forge.platform.intelligence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RepositorySnapshotTest {

  private static final BuildCoordinates COORDINATES =
      new BuildCoordinates("io.forge.platform", "forge-ai", "0.1.0");

  @Test
  void preservesFieldsExactly() {
    List<PackageSummary> packages = List.of(new PackageSummary("io.forge.platform.core", 3));
    Set<PackageDependency> dependencies =
        Set.of(new PackageDependency("io.forge.platform.core.id", "io.forge.platform.core"));

    RepositorySnapshot snapshot = new RepositorySnapshot(COORDINATES, 25, packages, dependencies);

    assertEquals(COORDINATES, snapshot.coordinates());
    assertEquals(25, snapshot.javaVersion());
    assertEquals(packages, snapshot.packages());
    assertEquals(dependencies, snapshot.internalDependencies());
  }

  @Test
  void defensivelyCopiesPackagesList() {
    List<PackageSummary> mutablePackages = new ArrayList<>();
    mutablePackages.add(new PackageSummary("pkg", 1));

    RepositorySnapshot snapshot =
        new RepositorySnapshot(COORDINATES, 25, mutablePackages, Set.of());
    mutablePackages.add(new PackageSummary("other", 1));

    assertEquals(1, snapshot.packages().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> snapshot.packages().add(new PackageSummary("third", 1)));
  }

  @Test
  void defensivelyCopiesInternalDependencies() {
    java.util.Set<PackageDependency> mutableDependencies = new java.util.HashSet<>();
    mutableDependencies.add(new PackageDependency("a", "b"));

    RepositorySnapshot snapshot =
        new RepositorySnapshot(COORDINATES, 25, List.of(), mutableDependencies);
    mutableDependencies.add(new PackageDependency("c", "d"));

    assertEquals(1, snapshot.internalDependencies().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> snapshot.internalDependencies().add(new PackageDependency("e", "f")));
  }

  @Test
  void rejectsNullCoordinates() {
    assertThrows(
        NullPointerException.class, () -> new RepositorySnapshot(null, 25, List.of(), Set.of()));
  }

  @Test
  void rejectsNonPositiveJavaVersion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RepositorySnapshot(COORDINATES, 0, List.of(), Set.of()));
  }

  @Test
  void rejectsNullPackages() {
    assertThrows(
        NullPointerException.class, () -> new RepositorySnapshot(COORDINATES, 25, null, Set.of()));
  }

  @Test
  void rejectsNullInternalDependencies() {
    assertThrows(
        NullPointerException.class, () -> new RepositorySnapshot(COORDINATES, 25, List.of(), null));
  }
}
