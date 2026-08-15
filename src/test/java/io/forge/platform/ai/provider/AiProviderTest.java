package io.forge.platform.ai.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.forge.platform.core.error.InfrastructureError;
import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import org.junit.jupiter.api.Test;

class AiProviderTest {

  @Test
  void fixedProviderAlwaysSucceedsWithTheSameCompletion() {
    AiCompletion completion = new AiCompletion("42");
    AiProvider provider = AiProvider.fixed(completion);

    Result<AiCompletion, PlatformError> first = provider.complete(new AiPrompt("What is 6*7?"));
    Result<AiCompletion, PlatformError> second =
        provider.complete(new AiPrompt("Unrelated prompt."));

    assertEquals(Result.success(completion), first);
    assertEquals(Result.success(completion), second);
  }

  @Test
  void fixedProviderRejectsNullCompletion() {
    assertThrows(NullPointerException.class, () -> AiProvider.fixed(null));
  }

  @Test
  void failingProviderAlwaysFailsWithTheSameError() {
    PlatformError error =
        InfrastructureError.of("ai.provider.unavailable", "Provider did not respond");
    AiProvider provider = AiProvider.failing(error);

    Result<AiCompletion, PlatformError> result = provider.complete(new AiPrompt("Any prompt."));

    assertEquals(Result.failure(error), result);
  }

  @Test
  void failingProviderRejectsNullError() {
    assertThrows(NullPointerException.class, () -> AiProvider.failing(null));
  }

  @Test
  void isUsableAsAFunctionalInterface() {
    AiProvider echoingLength =
        prompt -> Result.success(new AiCompletion(String.valueOf(prompt.text().length())));

    Result<AiCompletion, PlatformError> result = echoingLength.complete(new AiPrompt("hello"));

    assertTrue(result.isSuccess());
    assertEquals("5", result.fold(AiCompletion::text, error -> "unexpected failure"));
  }
}
