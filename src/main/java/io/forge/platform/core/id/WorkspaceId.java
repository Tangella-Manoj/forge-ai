package io.forge.platform.core.id;

import io.forge.platform.core.validation.Validation;
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
    Validation.requireNonNull(value, "value must not be null");
    Validation.requireTrue(
        InternalUuidGenerator.isVersion7(value), "WorkspaceId must wrap a UUIDv7");
  }
}
