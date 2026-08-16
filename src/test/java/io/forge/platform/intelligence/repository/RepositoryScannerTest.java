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
  void rejectsNullModuleRoot() {
    assertThrows(NullPointerException.class, () -> RepositoryScanner.scan(null));
  }
}
