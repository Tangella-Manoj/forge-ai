package io.forge.platform.intelligence.repository;

import io.forge.platform.core.validation.Validation;

/**
 * An observed fact: {@code fromPackage} imports at least one class from {@code toPackage}, and both
 * packages were found within the same scanned module — this deliberately excludes external (JDK,
 * framework, third-party) imports, which are module-level dependencies already declared in {@code
 * pom.xml}, not internal repository structure.
 *
 * @param fromPackage the package doing the importing
 * @param toPackage the package being imported from
 */
public record PackageDependency(String fromPackage, String toPackage) {

  public PackageDependency {
    Validation.requireNonBlank(fromPackage, "fromPackage must not be blank");
    Validation.requireNonBlank(toPackage, "toPackage must not be blank");
    Validation.requireTrue(!fromPackage.equals(toPackage), "fromPackage must not equal toPackage");
  }
}
