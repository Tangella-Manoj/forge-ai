package io.forge.platform.core.error;

import java.util.Map;
import java.util.Optional;

/**
 * Structured, explainable failure contract for Forge Platform core outcomes.
 *
 * <p>Every platform error carries a stable machine-readable code, a human-readable message,
 * optional machine-readable details, and an optional underlying cause for diagnostics. This is the
 * failure type core APIs use as the error channel of an explicit success/failure outcome.
 */
public sealed interface PlatformError permits DomainError, InfrastructureError {
  /**
   * Returns the stable, machine-readable error code.
   *
   * @return the error code
   */
  String code();

  /**
   * Returns the human-readable failure message.
   *
   * @return the failure message
   */
  String message();

  /**
   * Returns machine-readable structured context for the failure.
   *
   * @return an immutable map of structured details
   */
  Map<String, String> details();

  /**
   * Returns the underlying cause, when known.
   *
   * @return the underlying cause, or an empty optional when none is known
   */
  Optional<Throwable> cause();
}
