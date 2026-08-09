package io.forge.platform.core.id;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for a workspace.
 *
 * @param value the workspace UUIDv7 value
 */
public record WorkspaceId(UUID value) implements TypedId {
  /**
   * Creates a new workspace identifier.
   *
   * @return a new workspace identifier
   */
  public static WorkspaceId newId() {
    return of(InternalUuidGenerator.generate());
  }

  /**
   * Creates a workspace identifier from an existing UUIDv7 value.
   *
   * @param value the UUIDv7 value
   * @return a workspace identifier
   */
  public static WorkspaceId of(UUID value) {
    return new WorkspaceId(value);
  }

  public WorkspaceId {
    Objects.requireNonNull(value, "value must not be null");
    if (!InternalUuidGenerator.isVersion7(value)) {
      throw new IllegalArgumentException("WorkspaceId must wrap a UUIDv7");
    }
  }
}
