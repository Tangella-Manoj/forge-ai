package io.forge.platform.intelligence.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.forge.platform.intelligence.repository.BuildCoordinates;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EngineeringModelTest {

  private static RepositorySnapshot moduleNamed(String artifactId) {
    return new RepositorySnapshot(
        new BuildCoordinates("com.example", artifactId, "1.0.0"),
        21,
        List.of(),
        Set.of(),
        Set.of());
  }

  @Test
  void preservesFieldsExactly() {
    List<RepositorySnapshot> modules = List.of(moduleNamed("a"), moduleNamed("b"));
    Set<ModuleDependency> dependencies = Set.of(new ModuleDependency("a", "b"));

    EngineeringModel model = new EngineeringModel(modules, dependencies);

    assertEquals(modules, model.modules());
    assertEquals(dependencies, model.moduleDependencies());
  }

  @Test
  void rejectsNullModules() {
    assertThrows(NullPointerException.class, () -> new EngineeringModel(null, Set.of()));
  }

  @Test
  void rejectsNullModuleDependencies() {
    assertThrows(NullPointerException.class, () -> new EngineeringModel(List.of(), null));
  }

  @Test
  void dependentsOfFindsDirectDependents() {
    EngineeringModel model =
        new EngineeringModel(
            List.of(moduleNamed("common"), moduleNamed("loan-service")),
            Set.of(new ModuleDependency("loan-service", "common")));

    assertEquals(Set.of("loan-service"), model.dependentsOf("common"));
  }

  @Test
  void dependentsOfFindsTransitiveDependentsAcrossMultipleHops() {
    // gateway -> loan-service -> common: changing "common" affects both, transitively.
    EngineeringModel model =
        new EngineeringModel(
            List.of(moduleNamed("common"), moduleNamed("loan-service"), moduleNamed("gateway")),
            Set.of(
                new ModuleDependency("loan-service", "common"),
                new ModuleDependency("gateway", "loan-service")));

    assertEquals(Set.of("loan-service", "gateway"), model.dependentsOf("common"));
  }

  @Test
  void dependentsOfReturnsEmptyForAModuleNothingDependsOn() {
    EngineeringModel model =
        new EngineeringModel(
            List.of(moduleNamed("common"), moduleNamed("loan-service")),
            Set.of(new ModuleDependency("loan-service", "common")));

    assertTrue(model.dependentsOf("loan-service").isEmpty());
  }

  @Test
  void dependentsOfTerminatesOnACycleInsteadOfLoopingForever() {
    // A module-level cycle shouldn't normally exist (Maven itself would refuse to build it), but
    // the traversal must not assume that — verify it terminates rather than hanging, if one is
    // ever constructed some other way. In a true cycle (a -> b -> c -> a), "a" transitively
    // depends on itself, so it correctly appears in its own dependents set — not a bug.
    EngineeringModel model =
        new EngineeringModel(
            List.of(moduleNamed("a"), moduleNamed("b"), moduleNamed("c")),
            Set.of(
                new ModuleDependency("a", "b"),
                new ModuleDependency("b", "c"),
                new ModuleDependency("c", "a")));

    assertEquals(Set.of("a", "b", "c"), model.dependentsOf("a"));
  }

  @Test
  void dependentsOfHandlesAnUnknownArtifactIdGracefully() {
    EngineeringModel model = new EngineeringModel(List.of(), Set.of());

    assertTrue(model.dependentsOf("nonexistent").isEmpty());
  }

  @Test
  void dependentsOfRejectsBlankArtifactId() {
    EngineeringModel model = new EngineeringModel(List.of(), Set.of());

    assertThrows(IllegalArgumentException.class, () -> model.dependentsOf("  "));
  }
}
