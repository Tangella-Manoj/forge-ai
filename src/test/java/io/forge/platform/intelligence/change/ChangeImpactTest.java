package io.forge.platform.intelligence.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChangeImpactTest {

  @Test
  void preservesFieldsExactly() {
    ChangeImpact impact = new ChangeImpact("common", Set.of("loan-service"), Set.of("gateway"));

    assertEquals("common", impact.changedModule());
    assertEquals(Set.of("loan-service"), impact.directDependents());
    assertEquals(Set.of("gateway"), impact.transitiveDependents());
  }

  @Test
  void countsAffectedModulesAcrossBothCategories() {
    ChangeImpact impact = new ChangeImpact("common", Set.of("a", "b"), Set.of("c"));

    assertEquals(3, impact.affectedModuleCount());
  }

  @Test
  void countsZeroWhenNothingIsAffected() {
    assertEquals(0, new ChangeImpact("common", Set.of(), Set.of()).affectedModuleCount());
  }

  @Test
  void defensivelyCopiesBothDependentSets() {
    Set<String> mutableDirect = new HashSet<>(Set.of("a"));
    Set<String> mutableTransitive = new HashSet<>(Set.of("b"));

    ChangeImpact impact = new ChangeImpact("common", mutableDirect, mutableTransitive);
    mutableDirect.add("c");
    mutableTransitive.add("d");

    assertEquals(Set.of("a"), impact.directDependents());
    assertEquals(Set.of("b"), impact.transitiveDependents());
    assertThrows(UnsupportedOperationException.class, () -> impact.directDependents().add("e"));
    assertThrows(UnsupportedOperationException.class, () -> impact.transitiveDependents().add("f"));
  }

  @Test
  void rejectsBlankChangedModule() {
    assertThrows(IllegalArgumentException.class, () -> new ChangeImpact("  ", Set.of(), Set.of()));
  }

  @Test
  void rejectsNullFields() {
    assertThrows(NullPointerException.class, () -> new ChangeImpact(null, Set.of(), Set.of()));
    assertThrows(NullPointerException.class, () -> new ChangeImpact("common", null, Set.of()));
    assertThrows(NullPointerException.class, () -> new ChangeImpact("common", Set.of(), null));
  }
}
