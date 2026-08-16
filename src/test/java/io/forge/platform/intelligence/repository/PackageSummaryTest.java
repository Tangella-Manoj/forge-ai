package io.forge.platform.intelligence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PackageSummaryTest {

  @Test
  void preservesFieldsExactly() {
    PackageSummary summary = new PackageSummary("io.forge.platform.core.result", 1);

    assertEquals("io.forge.platform.core.result", summary.name());
    assertEquals(1, summary.classCount());
  }

  @Test
  void acceptsZeroClassCount() {
    assertEquals(0, new PackageSummary("empty.package", 0).classCount());
  }

  @Test
  void rejectsNegativeClassCount() {
    assertThrows(IllegalArgumentException.class, () -> new PackageSummary("pkg", -1));
  }

  @Test
  void rejectsBlankName() {
    assertThrows(IllegalArgumentException.class, () -> new PackageSummary("  ", 1));
  }

  @Test
  void rejectsNullName() {
    assertThrows(NullPointerException.class, () -> new PackageSummary(null, 1));
  }
}
