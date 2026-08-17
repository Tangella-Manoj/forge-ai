package io.forge.platform.reasoning;

import io.forge.platform.ai.provider.AiPrompt;
import io.forge.platform.ai.provider.AiProvider;
import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import io.forge.platform.core.validation.Validation;
import io.forge.platform.intelligence.architecture.CycleDetector;
import io.forge.platform.intelligence.architecture.CyclicPackageGroup;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reasons about a single module's architecture: builds an evidence list from a {@link
 * RepositorySnapshot} (via {@link CycleDetector}), sends it to a caller-supplied {@link
 * AiProvider}, and returns the evidence alongside the provider's narrative.
 *
 * <p>Deliberately takes {@code AiProvider} as a parameter rather than depending on any specific
 * implementation — this is the first real caller of the AI Runtime's provider abstraction outside
 * its own tests, and it stays entirely provider-neutral: swapping the fixed test double for a real
 * provider later requires no change here.
 */
public final class RepositoryAssessor {

  private RepositoryAssessor() {}

  /**
   * Assesses {@code snapshot}'s architecture using {@code provider}.
   *
   * @param snapshot the module to assess
   * @param provider the AI provider to reason with
   * @return the assessment, or a failure if the provider call failed
   */
  public static Result<ArchitectureAssessment, PlatformError> assess(
      RepositorySnapshot snapshot, AiProvider provider) {
    Validation.requireNonNull(snapshot, "snapshot must not be null");
    Validation.requireNonNull(provider, "provider must not be null");

    List<String> evidence = buildEvidence(snapshot);
    AiPrompt prompt = buildPrompt(snapshot, evidence);

    return provider
        .complete(prompt)
        .map(completion -> new ArchitectureAssessment(evidence, completion.text()));
  }

  private static List<String> buildEvidence(RepositorySnapshot snapshot) {
    List<String> evidence = new ArrayList<>();
    evidence.add(
        "Module "
            + snapshot.coordinates().artifactId()
            + " has "
            + snapshot.packages().size()
            + " packages and "
            + snapshot.internalDependencies().size()
            + " internal package dependencies.");

    Set<CyclicPackageGroup> cycles = CycleDetector.findCycles(snapshot.internalDependencies());
    if (cycles.isEmpty()) {
      evidence.add("No circular package dependencies were detected.");
    } else {
      for (CyclicPackageGroup cycle : cycles) {
        evidence.add(
            "Circular dependency detected among: "
                + String.join(", ", cycle.packages().stream().sorted().toList()));
      }
    }

    return List.copyOf(evidence);
  }

  private static AiPrompt buildPrompt(RepositorySnapshot snapshot, List<String> evidence) {
    String text =
        "You are assisting a software engineer reviewing the architecture of the module \""
            + snapshot.coordinates().artifactId()
            + "\". Given the following observed facts, write a concise assessment (2-4 sentences)"
            + " that highlights any real architectural risk and its likely impact. Do not restate"
            + " the facts verbatim; interpret them.\n\n"
            + String.join("\n", evidence);
    return new AiPrompt(text);
  }
}
