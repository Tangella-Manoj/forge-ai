package io.forge.platform.core.error;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Structured error representing a business rule or domain-level failure.
 *
 * <p>Example:
 *
 * <pre>{@code
 * PlatformError error =
 *     DomainError.of("workspace.name.blank", "Workspace name must not be blank");
 * }</pre>
 *
 * @param code stable, machine-readable error code
 * @param message human-readable description of the failure
 * @param details machine-readable structured context describing the failure
 * @param cause the underlying cause, when known
 */
public record DomainError(
    String code, String message, Map<String, String> details, Optional<Throwable> cause)
    implements PlatformError {

  /**
   * Creates a domain error with no structured details and no known cause.
   *
   * @param code stable, machine-readable error code
   * @param message human-readable description of the failure
   * @return a domain error
   */
  public static DomainError of(String code, String message) {
    return new DomainError(code, message, Map.of(), Optional.empty());
  }

  /**
   * Creates a domain error with structured details and no known cause.
   *
   * @param code stable, machine-readable error code
   * @param message human-readable description of the failure
   * @param details machine-readable structured context describing the failure
   * @return a domain error
   */
  public static DomainError of(String code, String message, Map<String, String> details) {
    return new DomainError(code, message, details, Optional.empty());
  }

  /**
   * Creates a domain error with a known cause and no structured details.
   *
   * @param code stable, machine-readable error code
   * @param message human-readable description of the failure
   * @param cause the underlying cause, or {@code null} when unknown
   * @return a domain error
   */
  public static DomainError of(String code, String message, Throwable cause) {
    return new DomainError(code, message, Map.of(), Optional.ofNullable(cause));
  }

  /**
   * Creates a domain error with structured details and a known cause.
   *
   * @param code stable, machine-readable error code
   * @param message human-readable description of the failure
   * @param details machine-readable structured context describing the failure
   * @param cause the underlying cause, or {@code null} when unknown
   * @return a domain error
   */
  public static DomainError of(
      String code, String message, Map<String, String> details, Throwable cause) {
    return new DomainError(code, message, details, Optional.ofNullable(cause));
  }

  public DomainError {
    Objects.requireNonNull(code, "code must not be null");
    if (code.isBlank()) {
      throw new IllegalArgumentException("code must not be blank");
    }
    Objects.requireNonNull(message, "message must not be null");
    if (message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
    Objects.requireNonNull(details, "details must not be null");
    Objects.requireNonNull(cause, "cause must not be null");
    details = Map.copyOf(details);
  }
}
