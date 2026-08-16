package io.forge.platform.intelligence.repository;

import io.forge.platform.core.validation.Validation;

/**
 * A deterministic fact about one Java package discovered in a scanned module: its name and how many
 * {@code .java} source files it directly contains (not counting subpackages).
 *
 * @param name the fully qualified package name
 * @param classCount the number of {@code .java} source files directly in this package
 */
public record PackageSummary(String name, int classCount) {

  public PackageSummary {
    Validation.requireNonBlank(name, "name must not be blank");
    Validation.requireTrue(classCount >= 0, "classCount must not be negative");
  }
}
