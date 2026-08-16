package io.forge.platform.intelligence.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CyclicPackageGroupTest {

  @Test
  void preservesPackagesExactly() {
    Set<String> packages = Set.of("a", "b");

    assertEquals(packages, new CyclicPackageGroup(packages).packages());
  }

  @Test
  void acceptsGroupsLargerThanTwo() {
    assertEquals(3, new CyclicPackageGroup(Set.of("a", "b", "c")).packages().size());
  }

  @Test
  void defensivelyCopiesPackages() {
    Set<String> mutable = new HashSet<>(Set.of("a", "b"));

    CyclicPackageGroup group = new CyclicPackageGroup(mutable);
    mutable.add("c");

    assertEquals(Set.of("a", "b"), group.packages());
    assertThrows(UnsupportedOperationException.class, () -> group.packages().add("c"));
  }

  @Test
  void rejectsFewerThanTwoPackages() {
    assertThrows(IllegalArgumentException.class, () -> new CyclicPackageGroup(Set.of("a")));
    assertThrows(IllegalArgumentException.class, () -> new CyclicPackageGroup(Set.of()));
  }

  @Test
  void rejectsNullPackages() {
    assertThrows(NullPointerException.class, () -> new CyclicPackageGroup(null));
  }
}
