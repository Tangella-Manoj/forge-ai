package io.forge.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The API's file-system trust boundary. These are security tests: a regression here would expose
 * arbitrary host directories to any caller, so each traversal shape is pinned explicitly rather
 * than assumed covered by a general case.
 */
class WorkspacePathResolverTest {

  private static WorkspacePathResolver resolverFor(Path root) {
    return new WorkspacePathResolver(new WorkspaceProperties(root.toString()));
  }

  @Test
  void resolvesTheRootItselfWhenNoPathIsGiven(@TempDir Path root) {
    WorkspacePathResolver resolver = resolverFor(root);

    assertEquals(root.toAbsolutePath().normalize(), resolver.resolve(null).fold(p -> p, e -> null));
    assertEquals(root.toAbsolutePath().normalize(), resolver.resolve("").fold(p -> p, e -> null));
    assertEquals(
        root.toAbsolutePath().normalize(), resolver.resolve("   ").fold(p -> p, e -> null));
  }

  @Test
  void resolvesARelativePathBeneathTheRoot(@TempDir Path root) {
    Result<Path, PlatformError> result = resolverFor(root).resolve("service-a");

    assertTrue(result.isSuccess());
    assertEquals(
        root.toAbsolutePath().normalize().resolve("service-a"), result.fold(p -> p, e -> null));
  }

  @Test
  void rejectsAnAbsolutePath(@TempDir Path root) {
    Result<Path, PlatformError> result = resolverFor(root).resolve("/etc");

    assertTrue(result.isFailure());
    assertEquals("workspace.path_must_be_relative", result.fold(p -> null, PlatformError::code));
  }

  @Test
  void rejectsTraversalAboveTheRoot(@TempDir Path root) {
    Result<Path, PlatformError> result = resolverFor(root).resolve("../../etc");

    assertTrue(result.isFailure());
    assertEquals("workspace.path_outside_root", result.fold(p -> null, PlatformError::code));
  }

  @Test
  void rejectsTraversalHiddenBehindAnInnocentPrefix(@TempDir Path root) {
    // The check runs on the *normalized* path precisely so this cannot slip through.
    Result<Path, PlatformError> result = resolverFor(root).resolve("service-a/../../../etc/passwd");

    assertTrue(result.isFailure());
    assertEquals("workspace.path_outside_root", result.fold(p -> null, PlatformError::code));
  }

  @Test
  void allowsTraversalThatStaysInsideTheRoot(@TempDir Path root) {
    // a/../b is just b — legitimate, and must not be rejected for merely containing "..".
    Result<Path, PlatformError> result = resolverFor(root).resolve("a/../b");

    assertTrue(result.isSuccess());
    assertEquals(root.toAbsolutePath().normalize().resolve("b"), result.fold(p -> p, e -> null));
  }

  @Test
  void relativizesPathsSoResponsesNeverLeakTheHostLayout(@TempDir Path root) {
    WorkspacePathResolver resolver = resolverFor(root);

    assertEquals(".", resolver.relativize(root.toAbsolutePath().normalize()));
    assertEquals(
        "service-a", resolver.relativize(root.toAbsolutePath().normalize().resolve("service-a")));
  }

  @Test
  void defaultsToTheWorkingDirectoryWhenRootIsUnset() {
    WorkspacePathResolver resolver = new WorkspacePathResolver(new WorkspaceProperties(null));

    assertEquals(
        Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize(), resolver.root());
  }
}
