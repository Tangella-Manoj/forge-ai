package io.forge.platform.intelligence.change;

import io.forge.platform.core.error.DomainError;
import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import io.forge.platform.core.validation.Validation;
import io.forge.platform.intelligence.model.EngineeringModel;
import io.forge.platform.intelligence.model.ModuleDependency;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Answers "if I change this module, what else is affected?" against an {@link EngineeringModel}.
 *
 * <p>Asking about a module that is not in the model is a {@link DomainError}, not an empty result:
 * "nothing depends on this module" and "you named a module this workspace does not contain" are
 * different answers, and silently returning the former for the latter would hide a typo or a stale
 * assumption behind a confident-looking zero.
 */
public final class ChangeImpactAnalyzer {

  private ChangeImpactAnalyzer() {}

  /**
   * Analyzes the impact of changing {@code changedModule}.
   *
   * @param model the workspace's engineering model
   * @param changedModule the artifactId of the module being changed
   * @return the assessed impact, or a failure if {@code changedModule} is not part of {@code model}
   */
  public static Result<ChangeImpact, PlatformError> analyze(
      EngineeringModel model, String changedModule) {
    Validation.requireNonNull(model, "model must not be null");
    Validation.requireNonBlank(changedModule, "changedModule must not be blank");

    boolean moduleExists =
        model.modules().stream()
            .anyMatch(module -> module.coordinates().artifactId().equals(changedModule));
    if (!moduleExists) {
      return Result.failure(
          DomainError.of(
              "change.module_not_in_workspace",
              "No module named \"" + changedModule + "\" exists in this workspace"));
    }

    Set<String> allDependents = model.dependentsOf(changedModule);

    Set<String> direct = new LinkedHashSet<>();
    for (ModuleDependency dependency : model.moduleDependencies()) {
      if (dependency.toModule().equals(changedModule)) {
        direct.add(dependency.fromModule());
      }
    }

    Set<String> transitive = new LinkedHashSet<>(allDependents);
    transitive.removeAll(direct);

    return Result.success(new ChangeImpact(changedModule, direct, transitive));
  }
}
