package io.forge.platform.intelligence.architecture;

import io.forge.platform.core.validation.Validation;
import java.util.Set;

/**
 * A finding, not merely an observed fact: a group of two or more packages that are mutually
 * reachable from each other via internal imports — a circular dependency.
 *
 * <p>Deliberately reports the group of entangled packages rather than enumerating every individual
 * cycle path through them. A group of {@code n} mutually-reachable packages can contain
 * exponentially many distinct cycle paths; the actionable fact for an engineer is "these packages
 * are entangled and cannot be cleanly layered," not an exhaustive path listing.
 *
 * @param packages the packages participating in the cycle, at least two
 */
public record CyclicPackageGroup(Set<String> packages) {

  public CyclicPackageGroup {
    Validation.requireNonNull(packages, "packages must not be null");
    Validation.requireTrue(packages.size() >= 2, "a cyclic group must contain at least 2 packages");
    packages = Set.copyOf(packages);
  }
}
