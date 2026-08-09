package io.forge.platform.core.id;

import java.util.UUID;

/**
 * Marker contract for Forge Platform identity values.
 *
 * <p>Typed IDs are immutable and backed by UUIDv7.
 */
public interface TypedId {
  /**
   * Returns the underlying UUID value.
   *
   * @return the underlying UUID value
   */
  UUID value();
}
