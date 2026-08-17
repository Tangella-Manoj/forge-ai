package io.forge.platform.intelligence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryScannerTest {

  /**
   * Dogfooding: scans this repository's own module. If this ever fails, either the scanner or
   * pom.xml has drifted from what it claims to describe.
   */
  @Test
  void scansThisRepositoryCorrectly() {
    Path thisModuleRoot = Path.of(System.getProperty("user.dir"));

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(thisModuleRoot);

    assertTrue(result.isSuccess(), () -> "expected success, got: " + result);
    RepositorySnapshot snapshot = result.fold(value -> value, error -> null);

    assertEquals(
        new BuildCoordinates("io.forge.platform", "forge-ai", "0.1.0-SNAPSHOT"),
        snapshot.coordinates());
    assertEquals(25, snapshot.javaVersion());

    Optional<PackageSummary> resultPackage =
        snapshot.packages().stream()
            .filter(p -> p.name().equals("io.forge.platform.core.result"))
            .findFirst();
    assertTrue(resultPackage.isPresent(), "expected to find io.forge.platform.core.result");
    assertEquals(1, resultPackage.get().classCount(), "Result.java is the only class there");

    assertTrue(snapshot.packages().size() >= 8, "expected at least the known core subpackages");

    // Real, verified facts about this repository's actual internal imports (checked via
    // grep before writing this assertion, not guessed).
    assertTrue(
        snapshot
            .internalDependencies()
            .contains(
                new PackageDependency(
                    "io.forge.platform.core.id", "io.forge.platform.core.validation")),
        "core.id imports core.validation");
    assertTrue(
        snapshot
            .internalDependencies()
            .contains(
                new PackageDependency("io.forge.platform.core.event", "io.forge.platform.core.id")),
        "core.event imports core.id");
    assertTrue(
        snapshot.internalDependencies().stream()
            .noneMatch(d -> d.fromPackage().equals("io.forge.platform.core.validation")),
        "core.validation imports only java.util.Objects — zero internal dependencies");

    // Real, verified facts about this repository's actual pom.xml <dependencies> (checked via
    // grep before writing this assertion, not guessed).
    assertEquals(
        Set.of(
            "spring-boot-starter",
            "spring-boot-starter-actuator",
            "spring-boot-starter-validation",
            "spring-boot-starter-test",
            "archunit-junit5"),
        snapshot.declaredDependencyArtifactIds());
  }

  @Test
  void failsWhenPomXmlIsMissing(@TempDir Path emptyDir) {
    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(emptyDir);

    assertTrue(result.isFailure());
    assertEquals("repository.scan.pom_missing", result.fold(v -> null, PlatformError::code));
  }

  @Test
  void failsWhenPomXmlIsMalformed(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("pom.xml"), "not valid xml <<<");

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(dir);

    assertTrue(result.isFailure());
    assertEquals("repository.scan.pom_unparseable", result.fold(v -> null, PlatformError::code));
  }

  @Test
  void failsWhenCoordinatesAreMissing(@TempDir Path dir) throws IOException {
    Files.writeString(
        dir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <properties>
            <maven.compiler.release>21</maven.compiler.release>
          </properties>
        </project>
        """);

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(dir);

    assertTrue(result.isFailure());
    assertEquals(
        "repository.scan.coordinates_missing", result.fold(v -> null, PlatformError::code));
  }

  @Test
  void failsWhenJavaVersionIsMissing(@TempDir Path dir) throws IOException {
    Files.writeString(
        dir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <groupId>com.example</groupId>
          <artifactId>demo</artifactId>
          <version>1.0.0</version>
        </project>
        """);

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(dir);

    assertTrue(result.isFailure());
    assertEquals(
        "repository.scan.java_version_missing", result.fold(v -> null, PlatformError::code));
  }

  @Test
  void succeedsWithEmptyPackagesWhenNoSourceDirectoryExists(@TempDir Path dir) throws IOException {
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

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(dir);

    assertTrue(result.isSuccess());
    RepositorySnapshot snapshot = result.fold(value -> value, error -> null);
    assertTrue(snapshot.packages().isEmpty());
    assertTrue(snapshot.internalDependencies().isEmpty());
    assertFalse(snapshot.packages() instanceof java.util.ArrayList);
  }

  @Test
  void fallsBackToJavaVersionPropertyWhenCompilerReleaseIsAbsent(@TempDir Path dir)
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
            <java.version>21</java.version>
          </properties>
        </project>
        """);

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(dir);

    assertTrue(result.isSuccess());
    assertEquals(21, result.fold(RepositorySnapshot::javaVersion, error -> -1));
  }

  @Test
  void failsWhenJavaVersionIsNotNumeric(@TempDir Path dir) throws IOException {
    Files.writeString(
        dir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <groupId>com.example</groupId>
          <artifactId>demo</artifactId>
          <version>1.0.0</version>
          <properties>
            <maven.compiler.release>not-a-number</maven.compiler.release>
          </properties>
        </project>
        """);

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(dir);

    assertTrue(result.isFailure());
    assertEquals(
        "repository.scan.java_version_missing", result.fold(v -> null, PlatformError::code));
  }

  @Test
  void failsWhenPropertiesElementHasNeitherJavaVersionTag(@TempDir Path dir) throws IOException {
    Files.writeString(
        dir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <groupId>com.example</groupId>
          <artifactId>demo</artifactId>
          <version>1.0.0</version>
          <properties>
            <unrelated.property>value</unrelated.property>
          </properties>
        </project>
        """);

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(dir);

    assertTrue(result.isFailure());
    assertEquals(
        "repository.scan.java_version_missing", result.fold(v -> null, PlatformError::code));
  }

  @Test
  void resolvesRegularWildcardAndStaticImportsAgainstKnownPackagesOnly(@TempDir Path dir)
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

    Path appDir = Files.createDirectories(dir.resolve("src/main/java/com/example/app"));
    Path utilDir = Files.createDirectories(dir.resolve("src/main/java/com/example/util"));
    Path modelDir = Files.createDirectories(dir.resolve("src/main/java/com/example/model"));

    Files.writeString(
        appDir.resolve("App.java"),
        """
        package com.example.app;

        import com.example.util.Helper;
        import com.example.model.*;
        import static com.example.util.Constants.MAX;
        import java.util.List;
        import static java.util.Collections.emptyList;

        class App {}
        """);
    Files.writeString(
        utilDir.resolve("Helper.java"), "package com.example.util;\nclass Helper {}\n");
    Files.writeString(modelDir.resolve("Item.java"), "package com.example.model;\nclass Item {}\n");

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(dir);

    assertTrue(result.isSuccess());
    RepositorySnapshot snapshot = result.fold(value -> value, error -> null);

    assertEquals(
        Set.of(
            new PackageDependency("com.example.app", "com.example.util"),
            new PackageDependency("com.example.app", "com.example.model")),
        snapshot.internalDependencies(),
        "regular, wildcard, and static-member imports resolve to their package; "
            + "java.util imports (external) are excluded");
  }

  @Test
  void rejectsNullModuleRoot() {
    assertThrows(NullPointerException.class, () -> RepositoryScanner.scan(null));
  }

  // --- declaredDependencyArtifactIds ---

  @Test
  void extractsDeclaredDependencyArtifactIds(@TempDir Path dir) throws IOException {
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
          <dependencies>
            <dependency>
              <groupId>com.example</groupId>
              <artifactId>common</artifactId>
            </dependency>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
          </dependencies>
        </project>
        """);

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(dir);

    assertTrue(result.isSuccess());
    assertEquals(
        Set.of("common", "spring-boot-starter-web"),
        result.fold(RepositorySnapshot::declaredDependencyArtifactIds, e -> null));
  }

  @Test
  void doesNotMistakeADependencyManagementBlockForRealDependencies(@TempDir Path dir)
      throws IOException {
    // Real bug, caught by checking against a real pom.xml before shipping: a naive
    // getElementsByTagName("dependencies") search finds a <dependencyManagement><dependencies>
    // block too (version pins, not real applied dependencies) if the file has no top-level
    // <dependencies> at all — exactly DLMP's own root pom.xml shape.
    Files.writeString(
        dir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <groupId>com.example</groupId>
          <artifactId>demo-parent</artifactId>
          <version>1.0.0</version>
          <properties>
            <maven.compiler.release>21</maven.compiler.release>
          </properties>
          <dependencyManagement>
            <dependencies>
              <dependency>
                <groupId>com.example</groupId>
                <artifactId>should-not-appear</artifactId>
                <version>1.0.0</version>
              </dependency>
            </dependencies>
          </dependencyManagement>
        </project>
        """);

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(dir);

    assertTrue(result.isSuccess());
    assertTrue(
        result.fold(RepositorySnapshot::declaredDependencyArtifactIds, e -> null).isEmpty(),
        "a dependencyManagement-only pom has no real applied dependencies of its own");
  }

  @Test
  void hasNoDeclaredDependencyArtifactIdsWhenNoDependenciesElementExists(@TempDir Path dir)
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

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(dir);

    assertTrue(result.isSuccess());
    assertTrue(result.fold(RepositorySnapshot::declaredDependencyArtifactIds, e -> null).isEmpty());
  }

  // --- Maven parent inheritance: verified against a real multi-module project (DLMP) before
  // these fixtures were written; a child module there declares only artifactId, inheriting
  // groupId/version from <parent> and java.version from the parent's own <properties> via the
  // default ../pom.xml relative path. These fixtures reproduce that exact shape synthetically so
  // the test suite has no dependency on another repository's location or existence.

  @Test
  void inheritsGroupIdAndVersionFromLocalParent(@TempDir Path dir) throws IOException {
    Files.writeString(
        dir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <groupId>com.example</groupId>
          <artifactId>parent</artifactId>
          <version>2.0.0</version>
          <packaging>pom</packaging>
          <properties>
            <maven.compiler.release>21</maven.compiler.release>
          </properties>
        </project>
        """);
    Path childDir = Files.createDirectories(dir.resolve("child"));
    Files.writeString(
        childDir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <parent>
            <groupId>com.example</groupId>
            <artifactId>parent</artifactId>
            <version>2.0.0</version>
          </parent>
          <artifactId>child</artifactId>
        </project>
        """);

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(childDir);

    assertTrue(result.isSuccess(), () -> "expected success, got: " + result);
    RepositorySnapshot snapshot = result.fold(value -> value, error -> null);
    assertEquals(new BuildCoordinates("com.example", "child", "2.0.0"), snapshot.coordinates());
  }

  @Test
  void inheritsJavaVersionFromLocalParentsPropertiesViaDefaultRelativePath(@TempDir Path dir)
      throws IOException {
    Files.writeString(
        dir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <groupId>com.example</groupId>
          <artifactId>parent</artifactId>
          <version>1.0.0</version>
          <packaging>pom</packaging>
          <properties>
            <java.version>21</java.version>
          </properties>
        </project>
        """);
    Path childDir = Files.createDirectories(dir.resolve("child"));
    Files.writeString(
        childDir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <parent>
            <groupId>com.example</groupId>
            <artifactId>parent</artifactId>
            <version>1.0.0</version>
          </parent>
          <artifactId>child</artifactId>
        </project>
        """);

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(childDir);

    assertTrue(result.isSuccess(), () -> "expected success, got: " + result);
    assertEquals(21, result.fold(RepositorySnapshot::javaVersion, error -> -1));
  }

  @Test
  void inheritsJavaVersionWhenRelativePathPointsAtADirectoryRatherThanAPomFile(@TempDir Path dir)
      throws IOException {
    Path parentDir = Files.createDirectories(dir.resolve("parent-module"));
    Files.writeString(
        parentDir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <groupId>com.example</groupId>
          <artifactId>parent</artifactId>
          <version>1.0.0</version>
          <packaging>pom</packaging>
          <properties>
            <maven.compiler.release>21</maven.compiler.release>
          </properties>
        </project>
        """);
    Path childDir = Files.createDirectories(dir.resolve("elsewhere/child"));
    Files.writeString(
        childDir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <parent>
            <groupId>com.example</groupId>
            <artifactId>parent</artifactId>
            <version>1.0.0</version>
            <relativePath>../../parent-module</relativePath>
          </parent>
          <artifactId>child</artifactId>
        </project>
        """);

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(childDir);

    assertTrue(result.isSuccess(), () -> "expected success, got: " + result);
    assertEquals(21, result.fold(RepositorySnapshot::javaVersion, error -> -1));
  }

  @Test
  void resolvesCoordinatesButNotJavaVersionFromARemoteParent(@TempDir Path dir) throws IOException {
    Files.writeString(
        dir.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <parent>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-parent</artifactId>
            <version>4.1.0</version>
            <relativePath/>
          </parent>
          <artifactId>demo</artifactId>
        </project>
        """);

    Result<RepositorySnapshot, PlatformError> result = RepositoryScanner.scan(dir);

    // groupId/version are stated inline on <parent> itself — no file read needed, so they resolve
    // even for a remote parent. java.version lives in the parent's *properties*, which requires
    // reading the parent's pom.xml — impossible for a remote (Maven Central) parent with an
    // explicitly empty <relativePath/>, so that specifically fails.
    assertTrue(result.isFailure());
    assertEquals(
        "repository.scan.java_version_missing", result.fold(v -> null, PlatformError::code));
  }

  // --- scanWorkspace: multi-module discovery ---

  @Test
  void scanWorkspaceReturnsRootAndEveryDeclaredModule(@TempDir Path dir) throws IOException {
    writeMinimalParentPom(dir, List.of("service-a", "service-b"));
    writeMinimalChildPom(dir, "service-a");
    writeMinimalChildPom(dir, "service-b");

    Result<List<RepositorySnapshot>, PlatformError> result = RepositoryScanner.scanWorkspace(dir);

    assertTrue(result.isSuccess(), () -> "expected success, got: " + result);
    List<RepositorySnapshot> snapshots = result.fold(value -> value, error -> null);
    assertEquals(3, snapshots.size(), "root + 2 declared modules");
    assertEquals("workspace-parent", snapshots.get(0).coordinates().artifactId());
    assertEquals("service-a", snapshots.get(1).coordinates().artifactId());
    assertEquals("service-b", snapshots.get(2).coordinates().artifactId());
  }

  @Test
  void scanWorkspaceReturnsJustTheRootWhenNoModulesAreDeclared(@TempDir Path dir)
      throws IOException {
    writeMinimalParentPom(dir, List.of());

    Result<List<RepositorySnapshot>, PlatformError> result = RepositoryScanner.scanWorkspace(dir);

    assertTrue(result.isSuccess());
    assertEquals(1, result.fold(value -> value, error -> null).size());
  }

  @Test
  void scanWorkspaceFailsWhenADeclaredModuleDoesNotExistOnDisk(@TempDir Path dir)
      throws IOException {
    writeMinimalParentPom(dir, List.of("missing-module"));

    Result<List<RepositorySnapshot>, PlatformError> result = RepositoryScanner.scanWorkspace(dir);

    assertTrue(result.isFailure());
    assertEquals("repository.scan.pom_missing", result.fold(v -> null, PlatformError::code));
  }

  @Test
  void rejectsNullWorkspaceRoot() {
    assertThrows(NullPointerException.class, () -> RepositoryScanner.scanWorkspace(null));
  }

  private static void writeMinimalParentPom(Path dir, List<String> modules) throws IOException {
    String modulesXml =
        modules.isEmpty()
            ? ""
            : "<modules>"
                + modules.stream()
                    .map(m -> "<module>" + m + "</module>")
                    .collect(java.util.stream.Collectors.joining())
                + "</modules>";
    Files.writeString(
        dir.resolve("pom.xml"),
        "<?xml version=\"1.0\"?><project>"
            + "<groupId>com.example</groupId><artifactId>workspace-parent</artifactId><version>1.0.0</version>"
            + "<packaging>pom</packaging>"
            + modulesXml
            + "<properties><maven.compiler.release>21</maven.compiler.release></properties>"
            + "</project>");
  }

  private static void writeMinimalChildPom(Path dir, String moduleName) throws IOException {
    Path childDir = Files.createDirectories(dir.resolve(moduleName));
    Files.writeString(
        childDir.resolve("pom.xml"),
        "<?xml version=\"1.0\"?><project>"
            + "<parent><groupId>com.example</groupId><artifactId>workspace-parent</artifactId><version>1.0.0</version></parent>"
            + "<artifactId>"
            + moduleName
            + "</artifactId>"
            + "</project>");
  }
}
