package io.forge.platform.ai.provider;

import io.forge.platform.core.validation.Validation;

/**
 * A vendor-agnostic request to an {@link AiProvider}.
 *
 * @param text the prompt text
 */
public record AiPrompt(String text) {

  public AiPrompt {
    Validation.requireNonBlank(text, "text must not be blank");
  }
}
