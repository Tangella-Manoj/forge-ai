package io.forge.platform.cli;

import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import io.forge.platform.intelligence.architecture.CycleDetector;
import io.forge.platform.intelligence.architecture.CyclicPackageGroup;
import io.forge.platform.intelligence.model.EngineeringModel;
import io.forge.platform.intelligence.model.EngineeringModelBuilder;
import io.forge.platform.intelligence.model.ModuleDependency;
import io.forge.platform.intelligence.repository.RepositoryScanner;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Renders a human-readable Repository + Architecture Intelligence report for a module or a whole
 * multi-module workspace — the smallest useful surface that makes {@link RepositoryScanner} and
 * {@link CycleDetector} (previously reachable only from unit tests) visible to an actual user.
 *
 * <p>Always scans via {@link RepositoryScanner#scanWorkspace(Path)} rather than branching on
 * whether the target declares Maven {@code <modules>}: a single-module project's parent has no
 * declared modules, so {@code scanWorkspace} naturally returns a one-element list — the
 * single-module and multi-module cases are the same code path, not two.
 *
 * <p>Deliberately framework-free and independently testable: the CLI adapter ({@code
 * RepositoryIntelligenceCli}) is a thin Spring {@code CommandLineRunner} that calls {@link
 * #generate(Path)} and handles process exit; all rendering logic lives here so it can be tested
 * directly, without a Spring context.
 */
final class RepositoryIntelligenceReport {

  private RepositoryIntelligenceReport() {}

  /**
   * Generates the report for the module or workspace rooted at {@code target}.
   *
   * @param target the module or workspace root to scan
   * @return the rendered report and whether the scan itself succeeded
   */
  static Outcome generate(Path target) {
    Result<List<RepositorySnapshot>, PlatformError> result =
        RepositoryScanner.scanWorkspace(target);

    return result.fold(
        snapshots -> new Outcome(renderSuccess(target, snapshots), true),
        error -> new Outcome(renderFailure(target, error), false));
  }

  private static String renderSuccess(Path target, List<RepositorySnapshot> snapshots) {
    StringBuilder report = new StringBuilder();
    report
        .append("Forge AI Platform — Repository Intelligence Report\n")
        .append("Scanned: ")
        .append(target.toAbsolutePath())
        .append('\n')
        .append("Modules: ")
        .append(snapshots.size())
        .append('\n');

    for (RepositorySnapshot snapshot : snapshots) {
      report.append('\n');
      appendModule(report, snapshot);
    }

    // Only meaningful with more than one module — a single module has nothing in the same
    // workspace to depend on, so the section would always be an empty, noisy line.
    if (snapshots.size() > 1) {
      report.append('\n');
      appendEngineeringModel(report, snapshots);
    }

    return report.toString();
  }

  private static void appendEngineeringModel(
      StringBuilder report, List<RepositorySnapshot> snapshots) {
    EngineeringModel model = EngineeringModelBuilder.build(snapshots);

    report.append("--- Module dependencies (within this workspace) ---\n");
    if (model.moduleDependencies().isEmpty()) {
      report.append("None detected.\n");
      return;
    }
    for (ModuleDependency dependency :
        model.moduleDependencies().stream()
            .sorted(
                Comparator.comparing(ModuleDependency::fromModule)
                    .thenComparing(ModuleDependency::toModule))
            .toList()) {
      report
          .append("  ")
          .append(dependency.fromModule())
          .append(" -> ")
          .append(dependency.toModule())
          .append('\n');
    }
  }

  private static void appendModule(StringBuilder report, RepositorySnapshot snapshot) {
    Set<CyclicPackageGroup> cycles = CycleDetector.findCycles(snapshot.internalDependencies());

    report
        .append("--- ")
        .append(snapshot.coordinates().groupId())
        .append(':')
        .append(snapshot.coordinates().artifactId())
        .append(':')
        .append(snapshot.coordinates().version())
        .append(" ---\n")
        .append("Java version: ")
        .append(snapshot.javaVersion())
        .append('\n')
        .append("Packages: ")
        .append(snapshot.packages().size())
        .append('\n')
        .append("Internal package dependencies: ")
        .append(snapshot.internalDependencies().size())
        .append('\n');

    if (cycles.isEmpty()) {
      report.append("Circular dependencies: none detected\n");
    } else {
      report
          .append("Circular dependencies: ")
          .append(cycles.size())
          .append(cycles.size() == 1 ? " group found\n" : " groups found\n");
      for (CyclicPackageGroup cycle : cycles) {
        report
            .append("  - ")
            .append(String.join(", ", cycle.packages().stream().sorted().toList()))
            .append('\n');
      }
    }
  }

  private static String renderFailure(Path target, PlatformError error) {
    return "Forge AI Platform — Repository Intelligence Report\n"
        + "Scanned: "
        + target.toAbsolutePath()
        + "\n"
        + "Scan failed: ["
        + error.code()
        + "] "
        + error.message()
        + "\n";
  }

  /**
   * The rendered report text, and whether the underlying scan succeeded.
   *
   * @param text the report, ready to print
   * @param succeeded whether the scan succeeded (independent of whether cycles were found — a clean
   *     scan that finds cycles still succeeded; only an unreadable/unparseable module fails)
   */
  record Outcome(String text, boolean succeeded) {}
}
