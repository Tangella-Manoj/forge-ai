package io.forge.platform.core.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class ValidationTest {

  // --- requireNonNull ---

  @Test
  void requireNonNullReturnsValueWhenNonNull() {
    assertEquals("workspace", Validation.requireNonNull("workspace", "value must not be null"));
  }

  @Test
  void requireNonNullThrowsNullPointerExceptionWhenNull() {
    NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () -> Validation.requireNonNull(null, "value must not be null"));
    assertEquals("value must not be null", exception.getMessage());
  }

  // --- requireTrue ---

  @Test
  void requireTrueDoesNotThrowWhenConditionIsTrue() {
    Validation.requireTrue(true, "must be true");
  }

  @Test
  void requireTrueThrowsIllegalArgumentExceptionWhenConditionIsFalse() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> Validation.requireTrue(false, "must be true"));
    assertEquals("must be true", exception.getMessage());
  }

  // --- requireNonBlank ---

  @Test
  void requireNonBlankReturnsValueWhenNonBlank() {
    assertEquals("workspace", Validation.requireNonBlank("workspace", "must not be blank"));
  }

  @Test
  void requireNonBlankThrowsNullPointerExceptionWhenNull() {
    NullPointerException exception =
        assertThrows(
            NullPointerException.class,
            () -> Validation.requireNonBlank(null, "must not be blank"));
    assertEquals("must not be blank", exception.getMessage());
  }

  @Test
  void requireNonBlankThrowsIllegalArgumentExceptionWhenEmpty() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> Validation.requireNonBlank("", "must not be blank"));
    assertEquals("must not be blank", exception.getMessage());
  }

  @Test
  void requireNonBlankThrowsIllegalArgumentExceptionWhenWhitespaceOnly() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Validation.requireNonBlank("   ", "must not be blank"));
  }

  // --- shape: stateless, non-instantiable utility class ---

  @Test
  void classIsFinal() {
    assertTrue(Modifier.isFinal(Validation.class.getModifiers()));
  }

  @Test
  void constructorIsPrivate() throws NoSuchMethodException {
    Constructor<Validation> constructor = Validation.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()));
  }

  @Test
  void hasNoInstanceFields() {
    for (Field field : Validation.class.getDeclaredFields()) {
      assertTrue(Modifier.isStatic(field.getModifiers()));
    }
  }
}
