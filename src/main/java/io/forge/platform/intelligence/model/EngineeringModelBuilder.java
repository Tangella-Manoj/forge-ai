package io.forge.platform.intelligence.model;

import io.forge.platform.core.validation.Validation;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds an {@link EngineeringModel} from already-scanned modules — pure computation, no I/O, no
 * failure mode, since every fact it needs is already validated data sitting in the given {@link
 * RepositorySnapshot}s.
 *
 * <p>Resolves each module's raw {@code declaredDependencyArtifactIds} (collected by {@link
 * io.forge.platform.intelligence.repository.RepositoryScanner}, which cannot know on its own
 * whether a given artifactId belongs to a sibling module or an external library) against every
 * other module's artifactId in the same call — the same deferred-resolution pattern already used
 * for {@link io.forge.platform.intelligence.repository.PackageDependency}.
 */
public final class EngineeringModelBuilder {

  private EngineeringModelBuilder() {}

  /**
   * Builds the model for {@code modules}.
   *
   * @param modules every module in a workspace, typically from {@code
   *     RepositoryScanner.scanWorkspace}
   * @return the resulting model
   */
  public static EngineeringModel build(List<RepositorySnapshot> modules) {
    Validation.requireNonNull(modules, "modules must not be null");

    Set<String> moduleArtifactIds =
        modules.stream()
            .map(module -> module.coordinates().artifactId())
            .collect(Collectors.toUnmodifiableSet());

    Set<ModuleDependency> dependencies = new LinkedHashSet<>();
    for (RepositorySnapshot module : modules) {
      String fromModule = module.coordinates().artifactId();
      for (String declaredArtifactId : module.declaredDependencyArtifactIds()) {
        if (moduleArtifactIds.contains(declaredArtifactId)
            && !declaredArtifactId.equals(fromModule)) {
          dependencies.add(new ModuleDependency(fromModule, declaredArtifactId));
        }
      }
    }

    return new EngineeringModel(modules, dependencies);
  }
}
