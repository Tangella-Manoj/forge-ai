package io.forge.platform.core.id;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for a repository.
 *
 * @param value the repository UUIDv7 value
 */
public record RepositoryId(UUID value) implements TypedId {
  /**
   * Creates a new repository identifier.
   *
   * @return a new repository identifier
   */
  public static RepositoryId newId() {
    return of(InternalUuidGenerator.generate());
  }

  /**
   * Creates a repository identifier from an existing UUIDv7 value.
   *
   * @param value the UUIDv7 value
   * @return a repository identifier
   */
  public static RepositoryId of(UUID value) {
    return new RepositoryId(value);
  }

  public RepositoryId {
    Objects.requireNonNull(value, "value must not be null");
    if (!InternalUuidGenerator.isVersion7(value)) {
      throw new IllegalArgumentException("RepositoryId must wrap a UUIDv7");
    }
  }
}
