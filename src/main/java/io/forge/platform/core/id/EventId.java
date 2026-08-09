package io.forge.platform.core.id;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for a domain event.
 *
 * @param value the event UUIDv7 value
 */
public record EventId(UUID value) implements TypedId {
  /**
   * Creates a new event identifier.
   *
   * @return a new event identifier
   */
  public static EventId newId() {
    return of(InternalUuidGenerator.generate());
  }

  /**
   * Creates an event identifier from an existing UUIDv7 value.
   *
   * @param value the UUIDv7 value
   * @return an event identifier
   */
  public static EventId of(UUID value) {
    return new EventId(value);
  }

  public EventId {
    Objects.requireNonNull(value, "value must not be null");
    if (!InternalUuidGenerator.isVersion7(value)) {
      throw new IllegalArgumentException("EventId must wrap a UUIDv7");
    }
  }
}
