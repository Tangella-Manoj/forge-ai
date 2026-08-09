package io.forge.platform.core.time;

import java.time.Instant;

/**
 * Production {@link Clock} implementation backed by the system UTC clock.
 *
 * <p>Package-private: reached only through {@link Clock#system()}, so the concrete strategy can
 * change later without affecting any caller of {@link Clock}.
 */
final class SystemClock implements Clock {
  @Override
  public Instant now() {
    return Instant.now();
  }
}
