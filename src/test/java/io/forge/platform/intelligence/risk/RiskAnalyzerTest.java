package io.forge.platform.intelligence.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.forge.platform.intelligence.model.EngineeringModel;
import io.forge.platform.intelligence.model.ModuleDependency;
import io.forge.platform.intelligence.repository.BuildCoordinates;
import io.forge.platform.intelligence.repository.PackageDependency;
import io.forge.platform.intelligence.repository.PackageSummary;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RiskAnalyzerTest {

  private static RepositorySnapshot module(
      String artifactId, Set<PackageDependency> internalDependencies) {
    return new RepositorySnapshot(
        new BuildCoordinates("com.example", artifactId, "1.0.0"),
        21,
        List.of(new PackageSummary("com.example." + artifactId, 1)),
        internalDependencies,
        Set.of());
  }

  private static RepositorySnapshot module(String artifactId) {
    return module(artifactId, Set.of());
  }

  // --- circular package dependencies ---

  @Test
  void reportsCircularPackageDependencyAsHigh() {
    EngineeringModel model =
        new EngineeringModel(
            List.of(
                module(
                    "svc",
                    Set.of(new PackageDependency("a", "b"), new PackageDependency("b", "a")))),
            Set.of());

    List<RiskFinding> findings = RiskAnalyzer.analyze(model);

    assertEquals(1, findings.size());
    RiskFinding finding = findings.get(0);
    assertEquals(RiskCategory.CIRCULAR_PACKAGE_DEPENDENCY, finding.category());
    assertEquals(RiskSeverity.HIGH, finding.severity());
    assertEquals("svc", finding.subject());
    assertTrue(finding.evidence().stream().anyMatch(e -> e.contains("a, b")));
  }

  @Test
  void reportsNoFindingForAnAcyclicModule() {
    EngineeringModel model =
        new EngineeringModel(
            List.of(module("svc", Set.of(new PackageDependency("a", "b")))), Set.of());

    assertTrue(RiskAnalyzer.analyze(model).isEmpty());
  }

  // --- circular module dependencies ---

  @Test
  void reportsCircularModuleDependencyAsHigh() {
    EngineeringModel model =
        new EngineeringModel(
            List.of(module("a"), module("b")),
            Set.of(new ModuleDependency("a", "b"), new ModuleDependency("b", "a")));

    List<RiskFinding> findings =
        RiskAnalyzer.analyze(model).stream()
            .filter(f -> f.category() == RiskCategory.CIRCULAR_MODULE_DEPENDENCY)
            .toList();

    assertEquals(2, findings.size(), "both modules are in the cycle");
    assertTrue(findings.stream().allMatch(f -> f.severity() == RiskSeverity.HIGH));
  }

  // --- change amplification ---

  @Test
  void reportsChangeAmplificationAsMediumNeverHigh() {
    // A shared module with many dependents is normal, intentional design — reporting it as HIGH
    // would flag correct architecture as broken. This test pins that judgment.
    EngineeringModel model =
        new EngineeringModel(
            List.of(module("common"), module("a"), module("b"), module("c")),
            Set.of(
                new ModuleDependency("a", "common"),
                new ModuleDependency("b", "common"),
                new ModuleDependency("c", "common")));

    List<RiskFinding> findings = RiskAnalyzer.analyze(model);

    assertEquals(1, findings.size());
    RiskFinding finding = findings.get(0);
    assertEquals(RiskCategory.CHANGE_AMPLIFICATION, finding.category());
    assertEquals(RiskSeverity.MEDIUM, finding.severity());
    assertEquals("common", finding.subject());
    assertTrue(finding.evidence().stream().anyMatch(e -> e.contains("affects 3 of 4 modules")));
    assertTrue(
        finding.reason().contains("not necessarily a design flaw"),
        "the reason must not imply a shared module is defective");
  }

  @Test
  void doesNotReportChangeAmplificationBelowTheThreshold() {
    // Two dependents is too common to be signal — nearly every shared module would qualify.
    EngineeringModel model =
        new EngineeringModel(
            List.of(module("common"), module("a"), module("b")),
            Set.of(new ModuleDependency("a", "common"), new ModuleDependency("b", "common")));

    assertTrue(RiskAnalyzer.analyze(model).isEmpty());
  }

  @Test
  void countsTransitiveDependentsTowardAmplification() {
    // d -> c -> b -> common: three modules are affected, only one directly.
    EngineeringModel model =
        new EngineeringModel(
            List.of(module("common"), module("b"), module("c"), module("d")),
            Set.of(
                new ModuleDependency("b", "common"),
                new ModuleDependency("c", "b"),
                new ModuleDependency("d", "c")));

    List<RiskFinding> findings =
        RiskAnalyzer.analyze(model).stream().filter(f -> f.subject().equals("common")).toList();

    assertEquals(1, findings.size());
    assertTrue(findings.get(0).evidence().stream().anyMatch(e -> e.contains("affects 3 of 4")));
  }

  // --- general behavior ---

  @Test
  void reportsNothingForACleanWorkspace() {
    EngineeringModel model =
        new EngineeringModel(
            List.of(module("common"), module("a")), Set.of(new ModuleDependency("a", "common")));

    assertTrue(RiskAnalyzer.analyze(model).isEmpty(), "a clean workspace must produce no noise");
  }

  @Test
  void reportsNothingForAnEmptyWorkspace() {
    assertTrue(RiskAnalyzer.analyze(new EngineeringModel(List.of(), Set.of())).isEmpty());
  }

  @Test
  void ordersFindingsBySeverityThenSubject() {
    EngineeringModel model =
        new EngineeringModel(
            List.of(
                module("common"),
                module("a"),
                module("b"),
                module(
                    "zz-cyclic",
                    Set.of(new PackageDependency("p", "q"), new PackageDependency("q", "p")))),
            Set.of(
                new ModuleDependency("a", "common"),
                new ModuleDependency("b", "common"),
                new ModuleDependency("zz-cyclic", "common")));

    List<RiskFinding> findings = RiskAnalyzer.analyze(model);

    assertEquals(2, findings.size());
    assertEquals(RiskSeverity.HIGH, findings.get(0).severity(), "HIGH sorts before MEDIUM");
    assertEquals("zz-cyclic", findings.get(0).subject());
    assertEquals(RiskSeverity.MEDIUM, findings.get(1).severity());
    assertEquals("common", findings.get(1).subject());
  }

  @Test
  void everyFindingCarriesEvidenceReasonAndRecommendation() {
    EngineeringModel model =
        new EngineeringModel(
            List.of(
                module(
                    "svc",
                    Set.of(new PackageDependency("a", "b"), new PackageDependency("b", "a"))),
                module("common"),
                module("x"),
                module("y")),
            Set.of(
                new ModuleDependency("svc", "common"),
                new ModuleDependency("x", "common"),
                new ModuleDependency("y", "common")));

    List<RiskFinding> findings = RiskAnalyzer.analyze(model);

    assertTrue(findings.size() >= 2);
    for (RiskFinding finding : findings) {
      assertTrue(!finding.evidence().isEmpty(), "every finding must be checkable");
      assertTrue(!finding.reason().isBlank());
      assertTrue(!finding.recommendation().isBlank());
    }
  }

  @Test
  void rejectsNullModel() {
    assertThrows(NullPointerException.class, () -> RiskAnalyzer.analyze(null));
  }
}
