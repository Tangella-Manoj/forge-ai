package io.forge.platform.core.validation;

import java.util.Objects;

/**
 * Shared kernel-level precondition checks for IDs, value objects, and other platform primitives.
 *
 * <p>This is not a general-purpose validation framework. It covers only the primitive
 * invariant/precondition checks duplicated across kernel types — non-null, boolean invariants, and
 * non-blank strings. It intentionally does not grow format-specific validators (email, regex,
 * range, URL, and similar); those belong at the boundary (Jakarta Validation) or in a concrete
 * domain type, not the kernel.
 */
public final class Validation {

  private Validation() {}

  /**
   * Requires that {@code value} is not {@code null}.
   *
   * @param value the value to check
   * @param message the exception message used when {@code value} is {@code null}
   * @param <T> the value type
   * @return {@code value}, when non-null
   * @throws NullPointerException if {@code value} is {@code null}
   */
  public static <T> T requireNonNull(T value, String message) {
    return Objects.requireNonNull(value, message);
  }

  /**
   * Requires that {@code condition} is {@code true}.
   *
   * @param condition the condition to check
   * @param message the exception message used when {@code condition} is {@code false}
   * @throws IllegalArgumentException if {@code condition} is {@code false}
   */
  public static void requireTrue(boolean condition, String message) {
    if (!condition) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Requires that {@code value} is not {@code null} and not blank.
   *
   * @param value the value to check
   * @param message the exception message used when {@code value} is blank
   * @return {@code value}, when non-null and non-blank
   * @throws NullPointerException if {@code value} is {@code null}
   * @throws IllegalArgumentException if {@code value} is blank
   */
  public static String requireNonBlank(String value, String message) {
    Objects.requireNonNull(value, message);
    if (value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
    return value;
  }
}
