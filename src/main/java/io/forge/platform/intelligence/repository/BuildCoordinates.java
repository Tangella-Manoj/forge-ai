package io.forge.platform.intelligence.repository;

import io.forge.platform.core.validation.Validation;

/**
 * A build system's identity for a single module — Maven's groupId/artifactId/version triple.
 *
 * @param groupId the build group identifier
 * @param artifactId the build artifact identifier
 * @param version the build version
 */
public record BuildCoordinates(String groupId, String artifactId, String version) {

  public BuildCoordinates {
    Validation.requireNonBlank(groupId, "groupId must not be blank");
    Validation.requireNonBlank(artifactId, "artifactId must not be blank");
    Validation.requireNonBlank(version, "version must not be blank");
  }
}
