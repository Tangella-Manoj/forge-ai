package io.forge.platform.intelligence.change;

import io.forge.platform.core.validation.Validation;
import java.util.Set;

/**
 * The assessed impact of changing one module: which other modules in the same workspace could be
 * affected, split by how they are affected.
 *
 * <p>{@code directDependents} is the strongest signal — those modules declare a build-time
 * dependency on the changed module and will recompile against it. {@code transitiveDependents} are
 * reached only through another module; they may still break, but the coupling is indirect. Keeping
 * them separate matters because they warrant different engineering attention, and merging them into
 * one number would discard exactly the distinction an engineer needs.
 *
 * @param changedModule the artifactId of the module being changed
 * @param directDependents modules declaring a direct dependency on {@code changedModule}
 * @param transitiveDependents modules affected only through one or more intermediate modules
 */
public record ChangeImpact(
    String changedModule, Set<String> directDependents, Set<String> transitiveDependents) {

  public ChangeImpact {
    Validation.requireNonBlank(changedModule, "changedModule must not be blank");
    Validation.requireNonNull(directDependents, "directDependents must not be null");
    Validation.requireNonNull(transitiveDependents, "transitiveDependents must not be null");
    directDependents = Set.copyOf(directDependents);
    transitiveDependents = Set.copyOf(transitiveDependents);
  }

  /**
   * Returns the total number of affected modules, direct and transitive.
   *
   * @return how many modules in this workspace are affected by the change
   */
  public int affectedModuleCount() {
    return directDependents.size() + transitiveDependents.size();
  }
}
