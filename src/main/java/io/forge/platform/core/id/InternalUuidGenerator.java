package io.forge.platform.core.id;

import io.forge.platform.core.validation.Validation;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Temporary internal UUID strategy for Forge Platform core identities.
 *
 * <p>This implementation is intentionally package-private and isolated from the public API. It can
 * be replaced later without changing any typed ID contracts if the platform adopts a JDK-native or
 * library-backed UUIDv7 implementation.
 */
final class InternalUuidGenerator {
  private InternalUuidGenerator() {}

  static UUID generate() {
    long timestampMillis = Instant.now().toEpochMilli();
    long mostSignificantBits = (timestampMillis & 0xFFFFFFFFFFFFL) << 16;
    mostSignificantBits |= 0x7000L | ThreadLocalRandom.current().nextLong(1L << 12);

    long leastSignificantBits = ThreadLocalRandom.current().nextLong() & 0x3FFFFFFFFFFFFFFFL;
    leastSignificantBits |= 0x8000000000000000L;

    return new UUID(mostSignificantBits, leastSignificantBits);
  }

  static boolean isVersion7(UUID value) {
    Validation.requireNonNull(value, "value must not be null");
    return value.version() == 7 && value.variant() == 2;
  }
}
