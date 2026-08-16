package io.forge.platform.cli;

import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import io.forge.platform.intelligence.architecture.CycleDetector;
import io.forge.platform.intelligence.architecture.CyclicPackageGroup;
import io.forge.platform.intelligence.repository.RepositoryScanner;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.nio.file.Path;
import java.util.Set;

/**
 * Renders a human-readable Repository + Architecture Intelligence report for a single module — the
 * smallest useful surface that makes {@link RepositoryScanner} and {@link CycleDetector}
 * (previously reachable only from unit tests) visible to an actual user.
 *
 * <p>Deliberately framework-free and independently testable: the CLI adapter ({@code
 * RepositoryIntelligenceCli}) is a thin Spring {@code CommandLineRunner} that calls {@link
 * #generate(Path)} and handles process exit; all rendering logic lives here so it can be tested
 * directly, without a Spring context.
 */
final class RepositoryIntelligenceReport {

  private RepositoryIntelligenceReport() {}

  /**
   * Generates the report for the module rooted at {@code target}.
   *
   * @param target the module root to scan
   * @return the rendered report and whether the scan itself succeeded
   */
  static Outcome generate(Path target) {
    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(target);

    return result.fold(
        snapshot -> new Outcome(renderSuccess(target, snapshot), true),
        error -> new Outcome(renderFailure(target, error), false));
  }

  private static String renderSuccess(Path target, RepositorySnapshot snapshot) {
    Set<CyclicPackageGroup> cycles = CycleDetector.findCycles(snapshot.internalDependencies());

    StringBuilder report = new StringBuilder();
    report
        .append("Forge AI Platform — Repository Intelligence Report\n")
        .append("Scanned: ")
        .append(target.toAbsolutePath())
        .append('\n')
        .append("Module: ")
        .append(snapshot.coordinates().groupId())
        .append(':')
        .append(snapshot.coordinates().artifactId())
        .append(':')
        .append(snapshot.coordinates().version())
        .append('\n')
        .append("Java version: ")
        .append(snapshot.javaVersion())
        .append('\n')
        .append("Packages: ")
        .append(snapshot.packages().size())
        .append('\n')
        .append("Internal package dependencies: ")
        .append(snapshot.internalDependencies().size())
        .append('\n')
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

    return report.toString();
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
