package io.forge.platform.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChangeImpactReportTest {

  /** gateway -> loan-service -> common, reproducing DLMP's real layering shape. */
  private static void writeThreeLayerWorkspace(Path dir) throws IOException {
    Files.writeString(
        dir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <groupId>com.example</groupId>
          <artifactId>workspace-parent</artifactId>
          <version>1.0.0</version>
          <packaging>pom</packaging>
          <modules>
            <module>common</module>
            <module>loan-service</module>
            <module>gateway</module>
          </modules>
          <properties>
            <maven.compiler.release>21</maven.compiler.release>
          </properties>
        </project>
        """);
    writeModule(dir, "common", null);
    writeModule(dir, "loan-service", "common");
    writeModule(dir, "gateway", "loan-service");
  }

  private static void writeModule(Path dir, String artifactId, String dependsOn)
      throws IOException {
    Path moduleDir = Files.createDirectories(dir.resolve(artifactId));
    String dependencies =
        dependsOn == null
            ? ""
            : """
              <dependencies>
                <dependency>
                  <groupId>com.example</groupId>
                  <artifactId>%s</artifactId>
                </dependency>
              </dependencies>
              """
                .formatted(dependsOn);
    Files.writeString(
        moduleDir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <parent>
            <groupId>com.example</groupId>
            <artifactId>workspace-parent</artifactId>
            <version>1.0.0</version>
          </parent>
          <artifactId>%s</artifactId>
          %s
        </project>
        """
            .formatted(artifactId, dependencies));
  }

  @Test
  void reportsDirectAndTransitiveDependents(@TempDir Path dir) throws IOException {
    writeThreeLayerWorkspace(dir);

    RepositoryIntelligenceReport.Outcome outcome = ChangeImpactReport.generate(dir, "common");

    assertTrue(outcome.succeeded(), () -> "expected success, got: " + outcome.text());
    assertTrue(outcome.text().contains("Changed module: common"));
    assertTrue(outcome.text().contains("Affected modules: 2"));
    assertTrue(outcome.text().contains("Direct dependents: 1"));
    assertTrue(outcome.text().contains("- loan-service"));
    assertTrue(outcome.text().contains("Transitive dependents: 1"));
    assertTrue(outcome.text().contains("- gateway"));
  }

  @Test
  void reportsAContainedChangeForALeafModule(@TempDir Path dir) throws IOException {
    writeThreeLayerWorkspace(dir);

    RepositoryIntelligenceReport.Outcome outcome = ChangeImpactReport.generate(dir, "gateway");

    assertTrue(outcome.succeeded());
    assertTrue(outcome.text().contains("Affected modules: 0"));
    assertTrue(outcome.text().contains("the change is contained to it"));
  }

  @Test
  void alwaysStatesTheModelsBuildTimeOnlyLimit(@TempDir Path dir) throws IOException {
    writeThreeLayerWorkspace(dir);

    RepositoryIntelligenceReport.Outcome outcome = ChangeImpactReport.generate(dir, "common");

    assertTrue(
        outcome.text().contains("build-time (Maven dependency) coupling only"),
        "the report must not let a confident number imply more than the model knows");
  }

  @Test
  void failsForAModuleNotInTheWorkspace(@TempDir Path dir) throws IOException {
    writeThreeLayerWorkspace(dir);

    RepositoryIntelligenceReport.Outcome outcome =
        ChangeImpactReport.generate(dir, "no-such-thing");

    assertFalse(outcome.succeeded());
    assertTrue(outcome.text().contains("change.module_not_in_workspace"));
  }

  @Test
  void failsWhenTheWorkspaceItselfCannotBeScanned(@TempDir Path emptyDir) {
    RepositoryIntelligenceReport.Outcome outcome = ChangeImpactReport.generate(emptyDir, "common");

    assertFalse(outcome.succeeded());
    assertTrue(outcome.text().contains("repository.scan.pom_missing"));
  }
}
