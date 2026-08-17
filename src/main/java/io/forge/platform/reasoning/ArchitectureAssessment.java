package io.forge.platform.reasoning;

import io.forge.platform.core.validation.Validation;
import java.util.List;

/**
 * The result of reasoning about a module's architecture: the deterministic facts it was reasoned
 * over, and the resulting narrative.
 *
 * <p>Keeps {@code evidence} and {@code narrative} deliberately distinct rather than merging them
 * into one blob of text — {@code evidence} is always an observed fact (from {@link
 * io.forge.platform.intelligence.repository.RepositoryScanner} / {@link
 * io.forge.platform.intelligence.architecture.CycleDetector}), while {@code narrative} is whatever
 * the configured {@link io.forge.platform.ai.provider.AiProvider} produced from it — an inference,
 * not a fact, and only as trustworthy as that provider. A caller (or a human) must always be able
 * to tell which is which.
 *
 * @param evidence the deterministic facts the assessment was reasoned over, in the order given to
 *     the provider
 * @param narrative the provider's completion text
 */
public record ArchitectureAssessment(List<String> evidence, String narrative) {

  public ArchitectureAssessment {
    Validation.requireNonNull(evidence, "evidence must not be null");
    Validation.requireNonNull(narrative, "narrative must not be null");
    evidence = List.copyOf(evidence);
  }
}
