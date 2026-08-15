package io.forge.platform.ai.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AiPromptTest {

  @Test
  void preservesTextExactly() {
    assertEquals("Explain this repository.", new AiPrompt("Explain this repository.").text());
  }

  @Test
  void rejectsNullText() {
    assertThrows(NullPointerException.class, () -> new AiPrompt(null));
  }

  @Test
  void rejectsBlankText() {
    assertThrows(IllegalArgumentException.class, () -> new AiPrompt("   "));
  }

  @Test
  void rejectsEmptyText() {
    assertThrows(IllegalArgumentException.class, () -> new AiPrompt(""));
  }
}
