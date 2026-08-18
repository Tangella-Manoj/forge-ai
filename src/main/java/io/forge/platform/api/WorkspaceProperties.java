package io.forge.platform.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the API is permitted to scan repositories.
 *
 * <p>Typed configuration rather than scattered {@code @Value} injection, so a missing or malformed
 * value fails at startup instead of at first request.
 *
 * @param root the only directory tree the API may read; every requested repository path is resolved
 *     beneath it and rejected if it escapes (see {@link WorkspacePathResolver}). Defaults to the
 *     process working directory, matching the CLI's own default.
 */
@ConfigurationProperties(prefix = "forge.workspace")
public record WorkspaceProperties(String root) {

  public WorkspaceProperties {
    if (root == null || root.isBlank()) {
      root = System.getProperty("user.dir");
    }
  }
}
