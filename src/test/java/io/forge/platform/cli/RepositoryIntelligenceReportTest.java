package io.forge.platform.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryIntelligenceReportTest {

  /** Dogfooding: the report renders correctly for this repository itself. */
  @Test
  void rendersASuccessfulReportForThisRepository() {
    Path thisModuleRoot = Path.of(System.getProperty("user.dir"));

    RepositoryIntelligenceReport.Outcome outcome =
        RepositoryIntelligenceReport.generate(thisModuleRoot);

    assertTrue(outcome.succeeded());
    assertTrue(outcome.text().contains("io.forge.platform:forge-ai:"));
    assertTrue(outcome.text().contains("Java version: 25"));
    assertTrue(outcome.text().contains("Circular dependencies: none detected"));
  }

  @Test
  void rendersAFailureMessageForAnUnscannableModule(@TempDir Path emptyDir) {
    RepositoryIntelligenceReport.Outcome outcome = RepositoryIntelligenceReport.generate(emptyDir);

    assertFalse(outcome.succeeded());
    assertTrue(outcome.text().contains("Scan failed"));
    assertTrue(outcome.text().contains("repository.scan.pom_missing"));
  }

  @Test
  void reportsCyclesWhenTheModuleHasThem(@TempDir Path dir) throws IOException {
    Files.writeString(
        dir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <groupId>com.example</groupId>
          <artifactId>demo</artifactId>
          <version>1.0.0</version>
          <properties>
            <maven.compiler.release>21</maven.compiler.release>
          </properties>
        </project>
        """);
    Path aDir = Files.createDirectories(dir.resolve("src/main/java/com/example/a"));
    Path bDir = Files.createDirectories(dir.resolve("src/main/java/com/example/b"));
    Files.writeString(
        aDir.resolve("A.java"), "package com.example.a;\nimport com.example.b.B;\nclass A {}\n");
    Files.writeString(
        bDir.resolve("B.java"), "package com.example.b;\nimport com.example.a.A;\nclass B {}\n");

    RepositoryIntelligenceReport.Outcome outcome = RepositoryIntelligenceReport.generate(dir);

    assertTrue(
        outcome.succeeded(), "a successful scan that happens to find a cycle is still a success");
    assertTrue(outcome.text().contains("Circular dependencies: 1 group found"));
    assertTrue(outcome.text().contains("com.example.a, com.example.b"));
  }

  @Test
  void pluralizesTheCircularDependenciesLineForMultipleGroups(@TempDir Path dir)
      throws IOException {
    Files.writeString(
        dir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <groupId>com.example</groupId>
          <artifactId>demo</artifactId>
          <version>1.0.0</version>
          <properties>
            <maven.compiler.release>21</maven.compiler.release>
          </properties>
        </project>
        """);
    Path aDir = Files.createDirectories(dir.resolve("src/main/java/com/example/a"));
    Path bDir = Files.createDirectories(dir.resolve("src/main/java/com/example/b"));
    Path xDir = Files.createDirectories(dir.resolve("src/main/java/com/example/x"));
    Path yDir = Files.createDirectories(dir.resolve("src/main/java/com/example/y"));
    Files.writeString(
        aDir.resolve("A.java"), "package com.example.a;\nimport com.example.b.B;\nclass A {}\n");
    Files.writeString(
        bDir.resolve("B.java"), "package com.example.b;\nimport com.example.a.A;\nclass B {}\n");
    Files.writeString(
        xDir.resolve("X.java"), "package com.example.x;\nimport com.example.y.Y;\nclass X {}\n");
    Files.writeString(
        yDir.resolve("Y.java"), "package com.example.y;\nimport com.example.x.X;\nclass Y {}\n");

    RepositoryIntelligenceReport.Outcome outcome = RepositoryIntelligenceReport.generate(dir);

    assertTrue(outcome.succeeded());
    assertTrue(outcome.text().contains("Circular dependencies: 2 groups found"));
  }

  @Test
  void succeedsWithNoCyclesReportedForAnAcyclicSyntheticModule(@TempDir Path dir)
      throws IOException {
    Files.writeString(
        dir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <groupId>com.example</groupId>
          <artifactId>demo</artifactId>
          <version>1.0.0</version>
          <properties>
            <maven.compiler.release>21</maven.compiler.release>
          </properties>
        </project>
        """);

    RepositoryIntelligenceReport.Outcome outcome = RepositoryIntelligenceReport.generate(dir);

    assertTrue(outcome.succeeded());
    assertTrue(outcome.text().contains("Packages: 0"));
    assertTrue(outcome.text().contains("Circular dependencies: none detected"));
  }

  @Test
  void rendersEveryModuleOfAMultiModuleWorkspace(@TempDir Path dir) throws IOException {
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
            <module>service-a</module>
            <module>service-b</module>
          </modules>
          <properties>
            <maven.compiler.release>21</maven.compiler.release>
          </properties>
        </project>
        """);
    for (String module : List.of("service-a", "service-b")) {
      Path moduleDir = Files.createDirectories(dir.resolve(module));
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
          </project>
          """
              .formatted(module));
    }

    RepositoryIntelligenceReport.Outcome outcome = RepositoryIntelligenceReport.generate(dir);

    assertTrue(outcome.succeeded());
    assertTrue(outcome.text().contains("Modules: 3"), "parent + 2 children");
    assertTrue(outcome.text().contains("com.example:workspace-parent:1.0.0"));
    assertTrue(outcome.text().contains("com.example:service-a:1.0.0"));
    assertTrue(outcome.text().contains("com.example:service-b:1.0.0"));
  }

  @Test
  void rendersRealModuleDependenciesBetweenServices(@TempDir Path dir) throws IOException {
    // Reproduces DLMP's real shape: a "common" module that another service module declares a
    // real Maven <dependency> on.
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
          </modules>
          <properties>
            <maven.compiler.release>21</maven.compiler.release>
          </properties>
        </project>
        """);
    Path commonDir = Files.createDirectories(dir.resolve("common"));
    Files.writeString(
        commonDir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <parent>
            <groupId>com.example</groupId>
            <artifactId>workspace-parent</artifactId>
            <version>1.0.0</version>
          </parent>
          <artifactId>common</artifactId>
        </project>
        """);
    Path loanServiceDir = Files.createDirectories(dir.resolve("loan-service"));
    Files.writeString(
        loanServiceDir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <parent>
            <groupId>com.example</groupId>
            <artifactId>workspace-parent</artifactId>
            <version>1.0.0</version>
          </parent>
          <artifactId>loan-service</artifactId>
          <dependencies>
            <dependency>
              <groupId>com.example</groupId>
              <artifactId>common</artifactId>
            </dependency>
          </dependencies>
        </project>
        """);

    RepositoryIntelligenceReport.Outcome outcome = RepositoryIntelligenceReport.generate(dir);

    assertTrue(outcome.succeeded());
    assertTrue(outcome.text().contains("Module dependencies (within this workspace)"));
    assertTrue(outcome.text().contains("loan-service -> common"));
  }
}
