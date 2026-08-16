package io.forge.platform.intelligence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PackageDependencyTest {

  @Test
  void preservesFieldsExactly() {
    PackageDependency dependency =
        new PackageDependency("io.forge.platform.core.id", "io.forge.platform.core.validation");

    assertEquals("io.forge.platform.core.id", dependency.fromPackage());
    assertEquals("io.forge.platform.core.validation", dependency.toPackage());
  }

  @Test
  void rejectsSelfDependency() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PackageDependency("io.forge.platform.core", "io.forge.platform.core"));
  }

  @Test
  void rejectsBlankFromPackage() {
    assertThrows(
        IllegalArgumentException.class, () -> new PackageDependency("  ", "io.forge.platform"));
  }

  @Test
  void rejectsBlankToPackage() {
    assertThrows(
        IllegalArgumentException.class, () -> new PackageDependency("io.forge.platform", "  "));
  }

  @Test
  void rejectsNullFields() {
    assertThrows(NullPointerException.class, () -> new PackageDependency(null, "io.forge"));
    assertThrows(NullPointerException.class, () -> new PackageDependency("io.forge", null));
  }
}
