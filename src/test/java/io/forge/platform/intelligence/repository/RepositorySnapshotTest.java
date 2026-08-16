package io.forge.platform.intelligence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepositorySnapshotTest {

  private static final BuildCoordinates COORDINATES =
      new BuildCoordinates("io.forge.platform", "forge-ai", "0.1.0");

  @Test
  void preservesFieldsExactly() {
    List<PackageSummary> packages = List.of(new PackageSummary("io.forge.platform.core", 3));

    RepositorySnapshot snapshot = new RepositorySnapshot(COORDINATES, 25, packages);

    assertEquals(COORDINATES, snapshot.coordinates());
    assertEquals(25, snapshot.javaVersion());
    assertEquals(packages, snapshot.packages());
  }

  @Test
  void defensivelyCopiesPackagesList() {
    List<PackageSummary> mutablePackages = new ArrayList<>();
    mutablePackages.add(new PackageSummary("pkg", 1));

    RepositorySnapshot snapshot = new RepositorySnapshot(COORDINATES, 25, mutablePackages);
    mutablePackages.add(new PackageSummary("other", 1));

    assertEquals(1, snapshot.packages().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> snapshot.packages().add(new PackageSummary("third", 1)));
  }

  @Test
  void rejectsNullCoordinates() {
    assertThrows(NullPointerException.class, () -> new RepositorySnapshot(null, 25, List.of()));
  }

  @Test
  void rejectsNonPositiveJavaVersion() {
    assertThrows(
        IllegalArgumentException.class, () -> new RepositorySnapshot(COORDINATES, 0, List.of()));
  }

  @Test
  void rejectsNullPackages() {
    assertThrows(NullPointerException.class, () -> new RepositorySnapshot(COORDINATES, 25, null));
  }
}
