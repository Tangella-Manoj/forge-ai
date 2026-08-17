package io.forge.platform.reasoning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.forge.platform.ai.provider.AiCompletion;
import io.forge.platform.ai.provider.AiPrompt;
import io.forge.platform.ai.provider.AiProvider;
import io.forge.platform.core.error.InfrastructureError;
import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import io.forge.platform.intelligence.repository.BuildCoordinates;
import io.forge.platform.intelligence.repository.PackageDependency;
import io.forge.platform.intelligence.repository.PackageSummary;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RepositoryAssessorTest {

  private static final BuildCoordinates COORDINATES =
      new BuildCoordinates("com.example", "demo", "1.0.0");

  @Test
  void assessesAnAcyclicModule() {
    RepositorySnapshot snapshot =
        new RepositorySnapshot(
            COORDINATES,
            21,
            List.of(new PackageSummary("com.example.demo", 3)),
            Set.of(),
            Set.of());
    AiProvider provider = AiProvider.fixed(new AiCompletion("Looks healthy."));

    Result<ArchitectureAssessment, PlatformError> result =
        RepositoryAssessor.assess(snapshot, provider);

    assertTrue(result.isSuccess());
    ArchitectureAssessment assessment = result.fold(value -> value, error -> null);
    assertEquals("Looks healthy.", assessment.narrative());
    assertTrue(assessment.evidence().get(0).contains("1 packages"));
    assertTrue(
        assessment.evidence().stream()
            .anyMatch(line -> line.contains("No circular package dependencies")));
  }

  @Test
  void assessesAModuleWithACycle() {
    RepositorySnapshot snapshot =
        new RepositorySnapshot(
            COORDINATES,
            21,
            List.of(new PackageSummary("a", 1), new PackageSummary("b", 1)),
            Set.of(new PackageDependency("a", "b"), new PackageDependency("b", "a")),
            Set.of());
    AiProvider provider = AiProvider.fixed(new AiCompletion("There is a tight coupling risk."));

    Result<ArchitectureAssessment, PlatformError> result =
        RepositoryAssessor.assess(snapshot, provider);

    assertTrue(result.isSuccess());
    ArchitectureAssessment assessment = result.fold(value -> value, error -> null);
    assertTrue(
        assessment.evidence().stream()
            .anyMatch(line -> line.contains("Circular dependency detected among: a, b")));
  }

  @Test
  void propagatesProviderFailure() {
    RepositorySnapshot snapshot =
        new RepositorySnapshot(COORDINATES, 21, List.of(), Set.of(), Set.of());
    PlatformError error =
        InfrastructureError.of("ai.provider.unavailable", "Provider did not respond");
    AiProvider provider = AiProvider.failing(error);

    Result<ArchitectureAssessment, PlatformError> result =
        RepositoryAssessor.assess(snapshot, provider);

    assertTrue(result.isFailure());
    assertEquals(error, result.fold(value -> null, e -> e));
  }

  @Test
  void sendsThePromptBuiltFromEvidence() {
    RepositorySnapshot snapshot =
        new RepositorySnapshot(
            COORDINATES,
            21,
            List.of(new PackageSummary("com.example.demo", 3)),
            Set.of(),
            Set.of());
    AtomicReference<AiPrompt> capturedPrompt = new AtomicReference<>();
    AiProvider capturingProvider =
        prompt -> {
          capturedPrompt.set(prompt);
          return Result.success(new AiCompletion("ok"));
        };

    RepositoryAssessor.assess(snapshot, capturingProvider);

    String promptText = capturedPrompt.get().text();
    assertTrue(promptText.contains("demo"), "prompt should mention the module's artifactId");
    assertTrue(
        promptText.contains("No circular package dependencies"),
        "prompt should embed the evidence, not just a summary");
  }

  @Test
  void rejectsNullSnapshot() {
    assertThrows(
        NullPointerException.class,
        () -> RepositoryAssessor.assess(null, AiProvider.fixed(new AiCompletion("x"))));
  }

  @Test
  void rejectsNullProvider() {
    RepositorySnapshot snapshot =
        new RepositorySnapshot(COORDINATES, 21, List.of(), Set.of(), Set.of());
    assertThrows(NullPointerException.class, () -> RepositoryAssessor.assess(snapshot, null));
  }
}
