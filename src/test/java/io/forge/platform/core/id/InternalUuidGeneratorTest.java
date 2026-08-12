package io.forge.platform.core.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class InternalUuidGeneratorTest {

  @Test
  void generatesVersion7Variant2Uuids() {
    UUID uuid = InternalUuidGenerator.generate();

    assertEquals(7, uuid.version());
    assertEquals(2, uuid.variant());
  }

  @Test
  void generatesDistinctUuidsSequentially() {
    Set<UUID> generated = ConcurrentHashMap.newKeySet();

    for (int i = 0; i < 10_000; i++) {
      generated.add(InternalUuidGenerator.generate());
    }

    assertEquals(10_000, generated.size());
  }

  @Test
  void generatesDistinctUuidsUnderConcurrency() throws Exception {
    int count = 10_000;
    Set<UUID> generated = ConcurrentHashMap.newKeySet();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures =
          IntStream.range(0, count)
              .<Future<?>>mapToObj(
                  i -> executor.submit(() -> generated.add(InternalUuidGenerator.generate())))
              .toList();

      for (Future<?> future : futures) {
        future.get();
      }
    }

    assertEquals(count, generated.size());
  }

  @Test
  void isVersion7AcceptsAVersion7Variant2Uuid() {
    assertTrue(InternalUuidGenerator.isVersion7(InternalUuidGenerator.generate()));
  }

  @Test
  void isVersion7RejectsCorrectVersionWithWrongVariant() {
    UUID uuid = InternalUuidGenerator.generate();
    // Force the variant field to the reserved NCS pattern (0xx) while keeping version 7 intact.
    long leastSignificantBits = uuid.getLeastSignificantBits() & 0x3FFFFFFFFFFFFFFFL;
    UUID wrongVariant = new UUID(uuid.getMostSignificantBits(), leastSignificantBits);

    assertEquals(7, wrongVariant.version());
    assertFalse(InternalUuidGenerator.isVersion7(wrongVariant));
  }

  @Test
  void isVersion7RejectsCorrectVariantWithWrongVersion() {
    UUID v4 = UUID.randomUUID();

    assertEquals(2, v4.variant());
    assertFalse(InternalUuidGenerator.isVersion7(v4));
  }

  @Test
  void isVersion7RejectsNull() {
    assertThrows(NullPointerException.class, () -> InternalUuidGenerator.isVersion7(null));
  }
}
