package io.forge.platform.intelligence.model;

import io.forge.platform.core.validation.Validation;

/**
 * An observed fact: {@code fromModule}'s {@code pom.xml} declares a {@code <dependency>} on {@code
 * toModule}, and both are modules within the same scanned workspace — this deliberately excludes
 * external (framework, third-party) dependencies, which are already visible in each module's own
 * {@code pom.xml} and are not workspace structure.
 *
 * <p>Captures build-time (compile/package) coupling only. A service that calls another over HTTP
 * (routing, service discovery) with no Maven dependency between them will not appear here — that is
 * a real, different kind of relationship this fact deliberately does not claim to know about.
 *
 * @param fromModule the module declaring the dependency
 * @param toModule the module being depended on
 */
public record ModuleDependency(String fromModule, String toModule) {

  public ModuleDependency {
    Validation.requireNonBlank(fromModule, "fromModule must not be blank");
    Validation.requireNonBlank(toModule, "toModule must not be blank");
    Validation.requireTrue(!fromModule.equals(toModule), "fromModule must not equal toModule");
  }
}
