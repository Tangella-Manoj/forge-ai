package io.forge.platform.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class RepositoryIntelligenceCliTest {

  @Test
  void printsNothingWhenNoArgsAreGiven() {
    assertTrue(capturedOutputOf(() -> RepositoryIntelligenceCli.execute()).isBlank());
  }

  @Test
  void printsNothingWhenTheFirstArgIsNotScan() {
    assertTrue(capturedOutputOf(() -> RepositoryIntelligenceCli.execute("status")).isBlank());
  }

  @Test
  void printsAReportWhenGivenTheScanCommand() {
    String output =
        capturedOutputOf(
            () -> RepositoryIntelligenceCli.execute("scan", System.getProperty("user.dir")));

    assertFalse(output.isBlank());
    assertTrue(output.contains("Repository Intelligence Report"));
  }

  @Test
  void defaultsToTheCurrentWorkingDirectoryWhenNoPathIsGiven() {
    String output = capturedOutputOf(() -> RepositoryIntelligenceCli.execute("scan"));

    assertFalse(output.isBlank());
    assertTrue(output.contains("io.forge.platform:forge-ai:"));
  }

  @Test
  void printsUsageWhenTheImpactCommandIsMissingItsModuleArgument() {
    String output = capturedOutputOf(() -> RepositoryIntelligenceCli.execute("impact"));

    assertTrue(output.contains("Usage: impact <module> [path]"));
  }

  @Test
  void treatsPrintedUsageAsSuccessNotFailure() {
    // Regression test for a real bug: returning usage text as a failure made run() call
    // System.exit(1), which killed the surefire JVM running this very class. Showing a user how
    // to invoke a command is normal operation, not an error.
    AtomicBoolean succeeded = new AtomicBoolean();
    capturedOutputOf(() -> succeeded.set(RepositoryIntelligenceCli.execute("impact")));

    assertTrue(succeeded.get());
  }

  @Test
  void reportsFailureForAnUnscannableTarget() {
    AtomicBoolean succeeded = new AtomicBoolean(true);
    capturedOutputOf(
        () -> succeeded.set(RepositoryIntelligenceCli.execute("scan", "/nonexistent-path-xyz")));

    assertFalse(succeeded.get());
  }

  @Test
  void printsAChangeImpactReportWhenGivenTheImpactCommand() {
    // This repository is a single module, so nothing depends on it — a real, correct answer.
    String output =
        capturedOutputOf(
            () ->
                new RepositoryIntelligenceCli()
                    .run("impact", "forge-ai", System.getProperty("user.dir")));

    assertTrue(output.contains("Change Impact Report"));
    assertTrue(output.contains("Changed module: forge-ai"));
    assertTrue(output.contains("Affected modules: 0"));
  }

  private static String capturedOutputOf(Runnable invocation) {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured));
    try {
      invocation.run();
    } finally {
      System.setOut(originalOut);
    }
    return captured.toString();
  }
}
