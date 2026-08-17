package io.forge.platform.intelligence.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import io.forge.platform.intelligence.model.EngineeringModel;
import io.forge.platform.intelligence.model.ModuleDependency;
import io.forge.platform.intelligence.repository.BuildCoordinates;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChangeImpactAnalyzerTest {

  private static RepositorySnapshot moduleNamed(String artifactId) {
    return new RepositorySnapshot(
        new BuildCoordinates("com.example", artifactId, "1.0.0"),
        21,
        List.of(),
        Set.of(),
        Set.of());
  }

  /** gateway -> loan-service -> common, reproducing DLMP's real layering shape. */
  private static EngineeringModel threeLayerModel() {
    return new EngineeringModel(
        List.of(moduleNamed("common"), moduleNamed("loan-service"), moduleNamed("gateway")),
        Set.of(
            new ModuleDependency("loan-service", "common"),
            new ModuleDependency("gateway", "loan-service")));
  }

  @Test
  void separatesDirectFromTransitiveDependents() {
    Result<ChangeImpact, PlatformError> result =
        ChangeImpactAnalyzer.analyze(threeLayerModel(), "common");

    assertTrue(result.isSuccess());
    ChangeImpact impact = result.fold(value -> value, error -> null);
    assertEquals(Set.of("loan-service"), impact.directDependents());
    assertEquals(Set.of("gateway"), impact.transitiveDependents());
    assertEquals(2, impact.affectedModuleCount());
  }

  @Test
  void reportsNoImpactForALeafModuleNothingDependsOn() {
    Result<ChangeImpact, PlatformError> result =
        ChangeImpactAnalyzer.analyze(threeLayerModel(), "gateway");

    assertTrue(result.isSuccess());
    ChangeImpact impact = result.fold(value -> value, error -> null);
    assertTrue(impact.directDependents().isEmpty());
    assertTrue(impact.transitiveDependents().isEmpty());
    assertEquals(0, impact.affectedModuleCount());
  }

  @Test
  void reportsOnlyDirectDependentsWhenThereIsNoDeeperChain() {
    Result<ChangeImpact, PlatformError> result =
        ChangeImpactAnalyzer.analyze(threeLayerModel(), "loan-service");

    assertTrue(result.isSuccess());
    ChangeImpact impact = result.fold(value -> value, error -> null);
    assertEquals(Set.of("gateway"), impact.directDependents());
    assertTrue(impact.transitiveDependents().isEmpty());
  }

  @Test
  void failsWhenTheModuleIsNotPartOfTheWorkspace() {
    // Deliberately a failure, not an empty result: "nothing depends on it" and "no such module"
    // are different answers, and conflating them would hide a typo behind a confident zero.
    Result<ChangeImpact, PlatformError> result =
        ChangeImpactAnalyzer.analyze(threeLayerModel(), "nonexistent-service");

    assertTrue(result.isFailure());
    assertEquals("change.module_not_in_workspace", result.fold(v -> null, PlatformError::code));
  }

  @Test
  void handlesFanOutWhereManyModulesShareOneDependency() {
    EngineeringModel model =
        new EngineeringModel(
            List.of(
                moduleNamed("common"),
                moduleNamed("user-service"),
                moduleNamed("loan-service"),
                moduleNamed("payment-service")),
            Set.of(
                new ModuleDependency("user-service", "common"),
                new ModuleDependency("loan-service", "common"),
                new ModuleDependency("payment-service", "common")));

    Result<ChangeImpact, PlatformError> result = ChangeImpactAnalyzer.analyze(model, "common");

    assertTrue(result.isSuccess());
    ChangeImpact impact = result.fold(value -> value, error -> null);
    assertEquals(
        Set.of("user-service", "loan-service", "payment-service"), impact.directDependents());
    assertTrue(impact.transitiveDependents().isEmpty());
  }

  @Test
  void rejectsNullModel() {
    assertThrows(NullPointerException.class, () -> ChangeImpactAnalyzer.analyze(null, "common"));
  }

  @Test
  void rejectsBlankChangedModule() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ChangeImpactAnalyzer.analyze(threeLayerModel(), "  "));
  }
}
