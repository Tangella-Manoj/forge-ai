package io.forge.platform.core.time;

import java.time.Instant;
import java.util.Objects;

/**
 * Test {@link Clock} implementation that always returns the same instant.
 *
 * <p>Package-private: reached only through {@link Clock#fixed(Instant)}, so the concrete strategy
 * can change later without affecting any caller of {@link Clock}.
 */
final class FixedClock implements Clock {
  private final Instant instant;

  FixedClock(Instant instant) {
    this.instant = Objects.requireNonNull(instant, "instant must not be null");
  }

  @Override
  public Instant now() {
    return instant;
  }
}
