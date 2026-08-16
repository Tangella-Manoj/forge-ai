package io.forge.platform.cli;

import java.nio.file.Path;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Thin Spring adapter exposing {@link RepositoryIntelligenceReport} as a command-line tool.
 *
 * <p>Usage: {@code java -jar forge-ai.jar scan [path]} — scans {@code path} (default: the current
 * working directory) and prints a Repository + Architecture Intelligence report.
 *
 * <p>Deliberately gated behind an explicit {@code scan} command rather than running
 * unconditionally: {@link CommandLineRunner}s fire on every Spring context startup, including this
 * application's own tests. An ungated version would run a full repository scan (and potentially
 * call {@link System#exit}) every time the context boots — harmless today, since this application
 * has no other purpose yet, but a real landmine once it gains one (a future web layer or
 * long-running service should not have an unrelated startup task able to kill it).
 *
 * <p>All rendering logic lives in {@link RepositoryIntelligenceReport}, which has no Spring
 * dependency and is tested directly; this class only wires it to process args/output/exit code.
 */
@Component
class RepositoryIntelligenceCli implements CommandLineRunner {

  private static final String SCAN_COMMAND = "scan";

  @Override
  public void run(String... args) {
    if (args.length == 0 || !SCAN_COMMAND.equals(args[0])) {
      return;
    }

    Path target = args.length > 1 ? Path.of(args[1]) : Path.of("").toAbsolutePath();

    RepositoryIntelligenceReport.Outcome outcome = RepositoryIntelligenceReport.generate(target);
    System.out.println(outcome.text());

    if (!outcome.succeeded()) {
      System.exit(1);
    }
  }
}
