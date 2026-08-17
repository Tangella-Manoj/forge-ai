package io.forge.platform.intelligence.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ModuleDependencyTest {

  @Test
  void preservesFieldsExactly() {
    ModuleDependency dependency = new ModuleDependency("loan-service", "common");

    assertEquals("loan-service", dependency.fromModule());
    assertEquals("common", dependency.toModule());
  }

  @Test
  void rejectsSelfDependency() {
    assertThrows(IllegalArgumentException.class, () -> new ModuleDependency("common", "common"));
  }

  @Test
  void rejectsBlankFromModule() {
    assertThrows(IllegalArgumentException.class, () -> new ModuleDependency("  ", "common"));
  }

  @Test
  void rejectsBlankToModule() {
    assertThrows(IllegalArgumentException.class, () -> new ModuleDependency("common", "  "));
  }

  @Test
  void rejectsNullFields() {
    assertThrows(NullPointerException.class, () -> new ModuleDependency(null, "common"));
    assertThrows(NullPointerException.class, () -> new ModuleDependency("loan-service", null));
  }
}
