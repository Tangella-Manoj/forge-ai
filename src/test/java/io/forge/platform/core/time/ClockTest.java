package io.forge.platform.core.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ClockTest {
  @Test
  void systemClockReturnsTheCurrentInstant() {
    Instant before = Instant.now();

    Instant result = Clock.system().now();

    Instant after = Instant.now();
    assertFalse(result.isBefore(before));
    assertFalse(result.isAfter(after));
  }

  @Test
  void systemClockReadingsAreNonDecreasing() {
    Clock clock = Clock.system();

    Instant first = clock.now();
    Instant second = clock.now();

    assertFalse(second.isBefore(first));
  }

  @Test
  void fixedClockAlwaysReturnsTheSameInstant() {
    Instant instant = Instant.parse("2026-01-01T00:00:00Z");
    Clock clock = Clock.fixed(instant);

    assertEquals(instant, clock.now());
    assertEquals(instant, clock.now());
  }

  @Test
  void fixedClockRejectsNullInstant() {
    assertThrows(NullPointerException.class, () -> Clock.fixed(null));
  }

  @Test
  void clockIsUsableAsAFunctionalInterface() {
    Instant instant = Instant.EPOCH;
    Clock clock = () -> instant;

    assertEquals(instant, clock.now());
  }

  @Test
  void systemAndFixedFactoriesReturnNonNullClocks() {
    assertNotNull(Clock.system());
    assertNotNull(Clock.fixed(Instant.now()));
  }
}
