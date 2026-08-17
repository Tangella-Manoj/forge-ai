package io.forge.platform.intelligence.risk;

import io.forge.platform.core.validation.Validation;
import io.forge.platform.intelligence.architecture.CycleDetector;
import io.forge.platform.intelligence.architecture.CyclicPackageGroup;
import io.forge.platform.intelligence.model.EngineeringModel;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Identifies engineering risks from evidence the Engineering Model and Architecture Intelligence
 * already produced. Pure computation — no I/O, no failure mode, no AI.
 *
 * <p>Every rule is a plain, stated condition on measured facts. There is deliberately no weighted
 * score, no composite index, and no learned model: a number like "risk score 7.3" would imply a
 * precision this analysis does not have and cannot explain, whereas "these two packages import each
 * other" is checkable by hand.
 *
 * <p><strong>Rules, in full:</strong>
 *
 * <ol>
 *   <li><em>Circular package dependency</em> (HIGH) — packages within a module that are mutually
 *       reachable. Objectively a defect: such packages cannot be layered, tested in isolation, or
 *       extracted independently, regardless of intent.
 *   <li><em>Circular module dependency</em> (HIGH) — modules mutually reachable through build
 *       dependencies. Maven itself cannot build these.
 *   <li><em>Change amplification</em> (MEDIUM, at {@value #AMPLIFICATION_THRESHOLD}+ affected
 *       modules) — changing one module affects a large share of the workspace. Capped at MEDIUM by
 *       design: a shared library legitimately has many dependents, so this is a cost signal, not a
 *       defect. Reporting it HIGH would flag correct, intentional architecture as broken.
 * </ol>
 */
public final class RiskAnalyzer {

  /**
   * Below this many affected modules, change amplification is too common to carry signal — nearly
   * every shared module in any workspace would qualify, which is the definition of a false
   * positive.
   */
  private static final int AMPLIFICATION_THRESHOLD = 3;

  private RiskAnalyzer() {}

  /**
   * Analyzes {@code model} and returns every finding, most severe first.
   *
   * @param model the workspace's engineering model
   * @return the findings, ordered by severity then subject; empty when no rule matched
   */
  public static List<RiskFinding> analyze(EngineeringModel model) {
    Validation.requireNonNull(model, "model must not be null");

    List<RiskFinding> findings = new ArrayList<>();
    for (RepositorySnapshot module : model.modules()) {
      findings.addAll(circularPackageFindings(module));
    }
    findings.addAll(circularModuleFindings(model));
    findings.addAll(changeAmplificationFindings(model));

    findings.sort(Comparator.comparing(RiskFinding::severity).thenComparing(RiskFinding::subject));
    return List.copyOf(findings);
  }

  private static List<RiskFinding> circularPackageFindings(RepositorySnapshot module) {
    String artifactId = module.coordinates().artifactId();
    List<RiskFinding> findings = new ArrayList<>();

    for (CyclicPackageGroup cycle : CycleDetector.findCycles(module.internalDependencies())) {
      List<String> packages = cycle.packages().stream().sorted().toList();
      findings.add(
          new RiskFinding(
              RiskCategory.CIRCULAR_PACKAGE_DEPENDENCY,
              RiskSeverity.HIGH,
              artifactId,
              List.of(
                  "Module \""
                      + artifactId
                      + "\" contains a package cycle among: "
                      + String.join(", ", packages),
                  "Each of these packages is reachable from every other via import statements."),
              "Mutually dependent packages cannot be layered, tested in isolation, or extracted"
                  + " into a separate module without moving all of them together. A change in any"
                  + " one can propagate to the others.",
              "Identify which direction the dependency should flow, and break the reverse edge —"
                  + " commonly by moving the shared type into a package both can depend on, or by"
                  + " inverting one dependency behind an interface."));
    }
    return findings;
  }

  private static List<RiskFinding> circularModuleFindings(EngineeringModel model) {
    List<RiskFinding> findings = new ArrayList<>();

    for (RepositorySnapshot module : model.modules()) {
      String artifactId = module.coordinates().artifactId();
      // A module reachable from itself is, by definition, part of a dependency cycle — reusing
      // the already-verified traversal rather than duplicating cycle detection here.
      if (!model.dependentsOf(artifactId).contains(artifactId)) {
        continue;
      }
      findings.add(
          new RiskFinding(
              RiskCategory.CIRCULAR_MODULE_DEPENDENCY,
              RiskSeverity.HIGH,
              artifactId,
              List.of(
                  "Module \""
                      + artifactId
                      + "\" is reachable from itself through build"
                      + " dependencies declared in pom.xml."),
              "Modules that depend on each other cyclically cannot be built independently or"
                  + " released separately; most build tools, Maven included, reject this outright.",
              "Break the cycle by extracting the shared types into a third module both can depend"
                  + " on."));
    }
    return findings;
  }

  private static List<RiskFinding> changeAmplificationFindings(EngineeringModel model) {
    List<RiskFinding> findings = new ArrayList<>();
    int totalModules = model.modules().size();

    for (RepositorySnapshot module : model.modules()) {
      String artifactId = module.coordinates().artifactId();
      Set<String> dependents = model.dependentsOf(artifactId);
      if (dependents.size() < AMPLIFICATION_THRESHOLD) {
        continue;
      }

      List<String> sortedDependents = dependents.stream().sorted().toList();
      findings.add(
          new RiskFinding(
              RiskCategory.CHANGE_AMPLIFICATION,
              RiskSeverity.MEDIUM,
              artifactId,
              List.of(
                  "Changing \""
                      + artifactId
                      + "\" affects "
                      + dependents.size()
                      + " of "
                      + totalModules
                      + " modules in this workspace.",
                  "Affected modules: " + String.join(", ", sortedDependents)),
              "A change here has a wide blast radius: every affected module recompiles against it"
                  + " and may need retesting. This is expected for a deliberately shared module —"
                  + " it is a cost to plan for, not necessarily a design flaw.",
              "Treat changes to this module as higher-risk than average: keep its public API"
                  + " stable, and verify the affected modules build and test cleanly before"
                  + " release."));
    }
    return findings;
  }
}
