package io.forge.platform.cli;

import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import io.forge.platform.intelligence.change.ChangeImpact;
import io.forge.platform.intelligence.change.ChangeImpactAnalyzer;
import io.forge.platform.intelligence.model.EngineeringModelBuilder;
import io.forge.platform.intelligence.repository.RepositoryScanner;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Renders a human-readable Change Intelligence report: "if I change this module, what else in this
 * workspace is affected?"
 *
 * <p>Same framework-free, independently-testable shape as {@link RepositoryIntelligenceReport} —
 * the Spring adapter only handles args, output, and exit code.
 */
final class ChangeImpactReport {

  private ChangeImpactReport() {}

  /**
   * Generates the change-impact report for {@code changedModule} within the workspace at {@code
   * target}.
   *
   * @param target the workspace root to scan
   * @param changedModule the artifactId of the module being changed
   * @return the rendered report and whether the analysis succeeded
   */
  static RepositoryIntelligenceReport.Outcome generate(Path target, String changedModule) {
    Result<List<RepositorySnapshot>, PlatformError> scan = RepositoryScanner.scanWorkspace(target);

    return scan.fold(
        snapshots ->
            ChangeImpactAnalyzer.analyze(EngineeringModelBuilder.build(snapshots), changedModule)
                .fold(
                    impact ->
                        new RepositoryIntelligenceReport.Outcome(
                            renderSuccess(target, impact), true),
                    error ->
                        new RepositoryIntelligenceReport.Outcome(
                            renderFailure(target, error), false)),
        error -> new RepositoryIntelligenceReport.Outcome(renderFailure(target, error), false));
  }

  private static String renderSuccess(Path target, ChangeImpact impact) {
    StringBuilder report = new StringBuilder();
    report
        .append("Forge AI Platform — Change Impact Report\n")
        .append("Workspace: ")
        .append(target.toAbsolutePath())
        .append('\n')
        .append("Changed module: ")
        .append(impact.changedModule())
        .append('\n')
        .append("Affected modules: ")
        .append(impact.affectedModuleCount())
        .append("\n\n");

    appendDependents(report, "Direct dependents", impact.directDependents());
    appendDependents(report, "Transitive dependents", impact.transitiveDependents());

    if (impact.affectedModuleCount() == 0) {
      report.append(
          "\nNothing in this workspace depends on this module — the change is contained to it.\n");
    }

    // State the model's limit rather than letting a confident-looking number imply more than it
    // knows: this is build-time coupling only (see ModuleDependency).
    report.append(
        "\nNote: build-time (Maven dependency) coupling only. Services calling each other over"
            + " HTTP without a declared dependency are not represented.\n");

    return report.toString();
  }

  private static void appendDependents(
      StringBuilder report, String heading, Set<String> dependents) {
    report.append(heading).append(": ").append(dependents.size()).append('\n');
    for (String dependent : dependents.stream().sorted().toList()) {
      report.append("  - ").append(dependent).append('\n');
    }
  }

  private static String renderFailure(Path target, PlatformError error) {
    return "Forge AI Platform — Change Impact Report\n"
        + "Workspace: "
        + target.toAbsolutePath()
        + "\n"
        + "Analysis failed: ["
        + error.code()
        + "] "
        + error.message()
        + "\n";
  }
}
