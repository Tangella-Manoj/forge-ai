package io.forge.platform.ai.provider;

import io.forge.platform.core.validation.Validation;

/**
 * A vendor-agnostic response from an {@link AiProvider}.
 *
 * <p>{@code text} may be empty (a model can legitimately return an empty completion) but never
 * {@code null}.
 *
 * @param text the completion text
 */
public record AiCompletion(String text) {

  public AiCompletion {
    Validation.requireNonNull(text, "text must not be null");
  }
}
