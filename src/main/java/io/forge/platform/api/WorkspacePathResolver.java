package io.forge.platform.api;

import io.forge.platform.core.error.DomainError;
import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import io.forge.platform.core.validation.Validation;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * The API's file-system trust boundary: turns a caller-supplied repository path into a real {@link
 * Path}, or refuses.
 *
 * <p>This exists because {@link io.forge.platform.intelligence.repository.RepositoryScanner} trusts
 * its {@code Path} argument completely — which is correct for a local CLI whose user already has a
 * shell, but would be an arbitrary-file-read vulnerability the moment a path arrives over HTTP. An
 * unguarded endpoint would let a caller confirm the existence of any directory on the host, read
 * any {@code pom.xml} on disk, and enumerate package names of unrelated projects, with absolute
 * paths echoed back in error messages.
 *
 * <p>Two rules, both enforced here:
 *
 * <ol>
 *   <li>The requested path must be relative — an absolute path is rejected outright rather than
 *       silently reinterpreted.
 *   <li>After normalization, the resolved path must still sit inside the configured root. This is
 *       what stops {@code ../../etc}, and it is checked on the <em>normalized</em> path so that
 *       traversal segments cannot hide behind an otherwise-innocent-looking prefix.
 * </ol>
 *
 * <p>Deliberately does not follow symlinks out of the root: {@link Path#normalize()} is lexical, so
 * a symlink inside the root pointing elsewhere would still be scanned. That is an accepted, stated
 * limit for now — the root is operator-configured, not attacker-controlled, so planting a symlink
 * already requires write access to the workspace. Revisit with {@code toRealPath()} if the root
 * ever becomes writable by untrusted users.
 */
@Component
class WorkspacePathResolver {

  private final Path root;

  WorkspacePathResolver(WorkspaceProperties properties) {
    this.root = Path.of(properties.root()).toAbsolutePath().normalize();
  }

  /**
   * Resolves {@code requestedPath} against the configured root.
   *
   * @param requestedPath a relative path; empty or {@code null} means the root itself
   * @return the resolved absolute path, or a failure explaining the refusal
   */
  Result<Path, PlatformError> resolve(String requestedPath) {
    if (requestedPath == null || requestedPath.isBlank()) {
      return Result.success(root);
    }

    Path candidate = Path.of(requestedPath);
    if (candidate.isAbsolute()) {
      return Result.failure(
          DomainError.of(
              "workspace.path_must_be_relative",
              "Repository path must be relative to the configured workspace root"));
    }

    Path resolved = root.resolve(candidate).normalize();
    if (!resolved.startsWith(root)) {
      return Result.failure(
          DomainError.of(
              "workspace.path_outside_root",
              "Repository path resolves outside the configured workspace root"));
    }

    return Result.success(resolved);
  }

  /**
   * Returns the configured root, for reporting which workspace an analysis covered.
   *
   * @return the absolute, normalized workspace root
   */
  Path root() {
    return root;
  }

  /**
   * Returns {@code absolutePath} expressed relative to the root — so responses never disclose the
   * host's absolute directory layout.
   *
   * @param absolutePath a path already validated as inside the root
   * @return the root-relative path, or {@code "."} for the root itself
   */
  String relativize(Path absolutePath) {
    Validation.requireNonNull(absolutePath, "absolutePath must not be null");
    String relative = root.relativize(absolutePath).toString();
    return relative.isEmpty() ? "." : relative;
  }
}
