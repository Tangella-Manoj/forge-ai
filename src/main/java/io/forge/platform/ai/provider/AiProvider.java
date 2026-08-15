package io.forge.platform.ai.provider;

import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import io.forge.platform.core.validation.Validation;

/**
 * Vendor-agnostic contract for sending a prompt to an AI model and receiving a completion.
 *
 * <p>This is the only type in {@code io.forge.platform.ai} permitted to have vendor-specific
 * implementations — every other AI Runtime capability depends on this contract rather than a
 * specific provider. No such implementation exists yet; connecting a real provider requires a
 * provider choice and API credentials, both outside this interface's scope.
 *
 * <p>Failures are expected outcomes (network faults, rate limits, provider errors), not exceptional
 * ones, so they are returned as a {@link Result} rather than thrown.
 */
@FunctionalInterface
public interface AiProvider {

  /**
   * Sends a prompt and returns the completion, or a failure describing why the call did not
   * succeed.
   *
   * @param prompt the prompt to send
   * @return the completion, or a failure
   */
  Result<AiCompletion, PlatformError> complete(AiPrompt prompt);

  /**
   * Returns a deterministic provider that always succeeds with the given completion, regardless of
   * the prompt. Intended for tests of code that depends on {@code AiProvider}.
   *
   * @param completion the completion every call returns
   * @return a fixed, always-succeeding provider
   */
  static AiProvider fixed(AiCompletion completion) {
    Validation.requireNonNull(completion, "completion must not be null");
    return prompt -> Result.success(completion);
  }

  /**
   * Returns a deterministic provider that always fails with the given error, regardless of the
   * prompt. Intended for tests of code that depends on {@code AiProvider}.
   *
   * @param error the error every call returns
   * @return a fixed, always-failing provider
   */
  static AiProvider failing(PlatformError error) {
    Validation.requireNonNull(error, "error must not be null");
    return prompt -> Result.failure(error);
  }
}
