package io.forge.platform.cli;

import java.nio.file.Path;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Thin Spring adapter exposing Forge's reports as a command-line tool.
 *
 * <p>Usage:
 *
 * <ul>
 *   <li>{@code java -jar forge-ai.jar scan [path]} — Repository + Architecture Intelligence report
 *       for {@code path} (default: the current working directory).
 *   <li>{@code java -jar forge-ai.jar impact <module> [path]} — Change Impact report: what else in
 *       the workspace is affected by changing {@code module}.
 * </ul>
 *
 * <p>Deliberately gated behind an explicit command rather than running unconditionally: {@link
 * CommandLineRunner}s fire on every Spring context startup, including this application's own tests.
 * An ungated version would run a full repository scan (and potentially call {@link System#exit})
 * every time the context boots — harmless today, since this application has no other purpose yet,
 * but a real landmine once it gains one (a future web layer or long-running service should not have
 * an unrelated startup task able to kill it).
 *
 * <p>All rendering logic lives in {@link RepositoryIntelligenceReport} and {@link
 * ChangeImpactReport}, which have no Spring dependency and are tested directly; this class only
 * wires them to process args/output/exit code.
 */
@Component
class RepositoryIntelligenceCli implements CommandLineRunner {

  private static final String SCAN_COMMAND = "scan";
  private static final String IMPACT_COMMAND = "impact";

  @Override
  public void run(String... args) {
    if (!execute(args)) {
      System.exit(1);
    }
  }

  /**
   * Runs the requested command and prints its output, returning whether it succeeded — the exit
   * code is the caller's decision, not this method's, so the whole command surface stays testable
   * without a test being able to kill its own JVM.
   *
   * @param args the process arguments
   * @return {@code true} if the command succeeded or no command applied; {@code false} on failure
   */
  static boolean execute(String... args) {
    if (args.length == 0) {
      return true;
    }

    RepositoryIntelligenceReport.Outcome outcome =
        switch (args[0]) {
          case SCAN_COMMAND -> RepositoryIntelligenceReport.generate(pathArgument(args, 1));
          case IMPACT_COMMAND -> runImpact(args);
          default -> null;
        };

    if (outcome == null) {
      return true;
    }

    System.out.println(outcome.text());
    return outcome.succeeded();
  }

  /**
   * Usage text is returned as a <em>successful</em> outcome deliberately: showing a user how to
   * invoke a command correctly is normal operation, not a failure worth a non-zero exit code. (An
   * earlier version returned it as a failure; that made the CLI's own test suite kill its JVM via
   * {@link System#exit} — a concrete demonstration of why "printed help" and "the analysis failed"
   * must not share an exit path.)
   */
  private static RepositoryIntelligenceReport.Outcome runImpact(String... args) {
    if (args.length < 2) {
      return new RepositoryIntelligenceReport.Outcome(
          "Usage: impact <module> [path]\n"
              + "  <module>  the artifactId of the module being changed\n"
              + "  [path]    the workspace root (default: current directory)\n",
          true);
    }
    return ChangeImpactReport.generate(pathArgument(args, 2), args[1]);
  }

  private static Path pathArgument(String[] args, int index) {
    return args.length > index ? Path.of(args[index]) : Path.of("").toAbsolutePath();
  }
}
