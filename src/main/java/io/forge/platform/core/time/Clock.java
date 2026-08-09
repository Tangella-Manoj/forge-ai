package io.forge.platform.core.time;

import java.time.Instant;

/**
 * Time abstraction for Forge Platform core logic.
 *
 * <p>Core logic must never read the current time directly (for example via {@code Instant.now()});
 * it depends on this abstraction instead, so that time-sensitive behavior stays deterministic and
 * testable.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Clock clock = Clock.system();
 * Instant createdAt = clock.now();
 * }</pre>
 */
@FunctionalInterface
public interface Clock {
  /**
   * Returns the current instant according to this clock.
   *
   * @return the current instant
   */
  Instant now();

  /**
   * Returns a clock backed by the system clock, in UTC.
   *
   * @return a system clock
   */
  static Clock system() {
    return new SystemClock();
  }

  /**
   * Returns a clock that always returns the given instant.
   *
   * <p>Intended for deterministic tests.
   *
   * @param instant the fixed instant to return
   * @return a fixed clock
   */
  static Clock fixed(Instant instant) {
    return new FixedClock(instant);
  }
}
