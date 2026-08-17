package io.forge.platform.intelligence.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.forge.platform.intelligence.repository.BuildCoordinates;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EngineeringModelBuilderTest {

  private static RepositorySnapshot moduleWithDependencies(
      String artifactId, Set<String> declaredDependencyArtifactIds) {
    return new RepositorySnapshot(
        new BuildCoordinates("com.example", artifactId, "1.0.0"),
        21,
        List.of(),
        Set.of(),
        declaredDependencyArtifactIds);
  }

  @Test
  void resolvesOnlyDependenciesThatMatchAnotherModuleInTheWorkspace() {
    // Reproduces DLMP's real shape: loan-service declares "common" (a workspace module) plus
    // several Spring Boot starters (external libraries) as dependencies.
    List<RepositorySnapshot> modules =
        List.of(
            moduleWithDependencies("common", Set.of("spring-boot-starter")),
            moduleWithDependencies(
                "loan-service",
                Set.of("common", "spring-boot-starter-web", "spring-boot-starter-data-jpa")));

    EngineeringModel model = EngineeringModelBuilder.build(modules);

    assertEquals(
        Set.of(new ModuleDependency("loan-service", "common")), model.moduleDependencies());
  }

  @Test
  void includesEveryModulePassedInRegardlessOfDependencies() {
    List<RepositorySnapshot> modules =
        List.of(moduleWithDependencies("a", Set.of()), moduleWithDependencies("b", Set.of()));

    EngineeringModel model = EngineeringModelBuilder.build(modules);

    assertEquals(modules, model.modules());
    assertTrue(model.moduleDependencies().isEmpty());
  }

  @Test
  void handlesAnEmptyWorkspace() {
    EngineeringModel model = EngineeringModelBuilder.build(List.of());

    assertTrue(model.modules().isEmpty());
    assertTrue(model.moduleDependencies().isEmpty());
  }

  @Test
  void doesNotProduceASelfDependencyEvenIfOddlyDeclared() {
    List<RepositorySnapshot> modules = List.of(moduleWithDependencies("a", Set.of("a")));

    EngineeringModel model = EngineeringModelBuilder.build(modules);

    assertTrue(model.moduleDependencies().isEmpty());
  }

  @Test
  void resolvesFanOutWhenMultipleModulesShareADependency() {
    // Reproduces DLMP's real shape further: several services all depend on the same "common".
    List<RepositorySnapshot> modules =
        List.of(
            moduleWithDependencies("common", Set.of()),
            moduleWithDependencies("user-service", Set.of("common")),
            moduleWithDependencies("loan-service", Set.of("common")),
            moduleWithDependencies("payment-service", Set.of("common")));

    EngineeringModel model = EngineeringModelBuilder.build(modules);

    assertEquals(
        Set.of("user-service", "loan-service", "payment-service"), model.dependentsOf("common"));
  }

  @Test
  void rejectsNullModules() {
    assertThrows(NullPointerException.class, () -> EngineeringModelBuilder.build(null));
  }
}
