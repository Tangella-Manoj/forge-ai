package io.forge.platform.intelligence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashSet;
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
    Set<String> declaredDependencies = Set.of("common");

    RepositorySnapshot snapshot =
        new RepositorySnapshot(COORDINATES, 25, packages, dependencies, declaredDependencies);

    assertEquals(COORDINATES, snapshot.coordinates());
    assertEquals(25, snapshot.javaVersion());
    assertEquals(packages, snapshot.packages());
    assertEquals(dependencies, snapshot.internalDependencies());
    assertEquals(declaredDependencies, snapshot.declaredDependencyArtifactIds());
  }

  @Test
  void defensivelyCopiesPackagesList() {
    List<PackageSummary> mutablePackages = new ArrayList<>();
    mutablePackages.add(new PackageSummary("pkg", 1));

    RepositorySnapshot snapshot =
        new RepositorySnapshot(COORDINATES, 25, mutablePackages, Set.of(), Set.of());
    mutablePackages.add(new PackageSummary("other", 1));

    assertEquals(1, snapshot.packages().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> snapshot.packages().add(new PackageSummary("third", 1)));
  }

  @Test
  void defensivelyCopiesInternalDependencies() {
    Set<PackageDependency> mutableDependencies = new HashSet<>();
    mutableDependencies.add(new PackageDependency("a", "b"));

    RepositorySnapshot snapshot =
        new RepositorySnapshot(COORDINATES, 25, List.of(), mutableDependencies, Set.of());
    mutableDependencies.add(new PackageDependency("c", "d"));

    assertEquals(1, snapshot.internalDependencies().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> snapshot.internalDependencies().add(new PackageDependency("e", "f")));
  }

  @Test
  void defensivelyCopiesDeclaredDependencyArtifactIds() {
    Set<String> mutableDeclared = new HashSet<>();
    mutableDeclared.add("common");

    RepositorySnapshot snapshot =
        new RepositorySnapshot(COORDINATES, 25, List.of(), Set.of(), mutableDeclared);
    mutableDeclared.add("other-module");

    assertEquals(1, snapshot.declaredDependencyArtifactIds().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> snapshot.declaredDependencyArtifactIds().add("third-module"));
  }

  @Test
  void rejectsNullCoordinates() {
    assertThrows(
        NullPointerException.class,
        () -> new RepositorySnapshot(null, 25, List.of(), Set.of(), Set.of()));
  }

  @Test
  void rejectsNonPositiveJavaVersion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RepositorySnapshot(COORDINATES, 0, List.of(), Set.of(), Set.of()));
  }

  @Test
  void rejectsNullPackages() {
    assertThrows(
        NullPointerException.class,
        () -> new RepositorySnapshot(COORDINATES, 25, null, Set.of(), Set.of()));
  }

  @Test
  void rejectsNullInternalDependencies() {
    assertThrows(
        NullPointerException.class,
        () -> new RepositorySnapshot(COORDINATES, 25, List.of(), null, Set.of()));
  }

  @Test
  void rejectsNullDeclaredDependencyArtifactIds() {
    assertThrows(
        NullPointerException.class,
        () -> new RepositorySnapshot(COORDINATES, 25, List.of(), Set.of(), null));
  }
}
