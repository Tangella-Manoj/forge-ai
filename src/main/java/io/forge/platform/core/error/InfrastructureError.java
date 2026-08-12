package io.forge.platform.core.error;

import io.forge.platform.core.validation.Validation;
import java.util.Map;
import java.util.Optional;

/**
 * Structured error representing an infrastructure-level failure, such as a database, network, or
 * external system fault.
 *
 * <p>Example:
 *
 * <pre>{@code
 * PlatformError error =
 *     InfrastructureError.of("database.connection.failed", "Could not reach the database", cause);
 * }</pre>
 *
 * @param code stable, machine-readable error code
 * @param message human-readable description of the failure
 * @param details machine-readable structured context describing the failure
 * @param cause the underlying cause, when known
 */
public record InfrastructureError(
    String code, String message, Map<String, String> details, Optional<Throwable> cause)
    implements PlatformError {

  /**
   * Creates an infrastructure error with no structured details and no known cause.
   *
   * @param code stable, machine-readable error code
   * @param message human-readable description of the failure
   * @return an infrastructure error
   */
  public static InfrastructureError of(String code, String message) {
    return new InfrastructureError(code, message, Map.of(), Optional.empty());
  }

  /**
   * Creates an infrastructure error with structured details and no known cause.
   *
   * @param code stable, machine-readable error code
   * @param message human-readable description of the failure
   * @param details machine-readable structured context describing the failure
   * @return an infrastructure error
   */
  public static InfrastructureError of(String code, String message, Map<String, String> details) {
    return new InfrastructureError(code, message, details, Optional.empty());
  }

  /**
   * Creates an infrastructure error with a known cause and no structured details.
   *
   * @param code stable, machine-readable error code
   * @param message human-readable description of the failure
   * @param cause the underlying cause, or {@code null} when unknown
   * @return an infrastructure error
   */
  public static InfrastructureError of(String code, String message, Throwable cause) {
    return new InfrastructureError(code, message, Map.of(), Optional.ofNullable(cause));
  }

  /**
   * Creates an infrastructure error with structured details and a known cause.
   *
   * @param code stable, machine-readable error code
   * @param message human-readable description of the failure
   * @param details machine-readable structured context describing the failure
   * @param cause the underlying cause, or {@code null} when unknown
   * @return an infrastructure error
   */
  public static InfrastructureError of(
      String code, String message, Map<String, String> details, Throwable cause) {
    return new InfrastructureError(code, message, details, Optional.ofNullable(cause));
  }

  public InfrastructureError {
    Validation.requireNonNull(code, "code must not be null");
    Validation.requireNonBlank(code, "code must not be blank");
    Validation.requireNonNull(message, "message must not be null");
    Validation.requireNonBlank(message, "message must not be blank");
    Validation.requireNonNull(details, "details must not be null");
    Validation.requireNonNull(cause, "cause must not be null");
    details = Map.copyOf(details);
  }
}
