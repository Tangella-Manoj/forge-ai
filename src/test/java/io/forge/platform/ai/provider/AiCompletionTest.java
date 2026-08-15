package io.forge.platform.ai.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AiCompletionTest {

  @Test
  void preservesTextExactly() {
    assertEquals("Here is the explanation.", new AiCompletion("Here is the explanation.").text());
  }

  @Test
  void acceptsEmptyText() {
    assertEquals("", new AiCompletion("").text());
  }

  @Test
  void rejectsNullText() {
    assertThrows(NullPointerException.class, () -> new AiCompletion(null));
  }
}
