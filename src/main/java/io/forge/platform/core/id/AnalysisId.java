package io.forge.platform.core.id;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for an analysis.
 *
 * @param value the analysis UUIDv7 value
 */
public record AnalysisId(UUID value) implements TypedId {
  /**
   * Creates a new analysis identifier.
   *
   * @return a new analysis identifier
   */
  public static AnalysisId newId() {
    return of(InternalUuidGenerator.generate());
  }

  /**
   * Creates an analysis identifier from an existing UUIDv7 value.
   *
   * @param value the UUIDv7 value
   * @return an analysis identifier
   */
  public static AnalysisId of(UUID value) {
    return new AnalysisId(value);
  }

  public AnalysisId {
    Objects.requireNonNull(value, "value must not be null");
    if (!InternalUuidGenerator.isVersion7(value)) {
      throw new IllegalArgumentException("AnalysisId must wrap a UUIDv7");
    }
  }
}
