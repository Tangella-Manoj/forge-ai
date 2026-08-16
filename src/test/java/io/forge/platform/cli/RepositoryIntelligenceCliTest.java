package io.forge.platform.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class RepositoryIntelligenceCliTest {

  @Test
  void printsNothingWhenNoArgsAreGiven() {
    assertTrue(capturedOutputOf(() -> new RepositoryIntelligenceCli().run()).isBlank());
  }

  @Test
  void printsNothingWhenTheFirstArgIsNotScan() {
    assertTrue(capturedOutputOf(() -> new RepositoryIntelligenceCli().run("status")).isBlank());
  }

  @Test
  void printsAReportWhenGivenTheScanCommand() {
    String output =
        capturedOutputOf(
            () -> new RepositoryIntelligenceCli().run("scan", System.getProperty("user.dir")));

    assertFalse(output.isBlank());
    assertTrue(output.contains("Repository Intelligence Report"));
  }

  @Test
  void defaultsToTheCurrentWorkingDirectoryWhenNoPathIsGiven() {
    String output = capturedOutputOf(() -> new RepositoryIntelligenceCli().run("scan"));

    assertFalse(output.isBlank());
    assertTrue(output.contains("io.forge.platform:forge-ai:"));
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
