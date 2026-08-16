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
}
