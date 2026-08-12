package io.forge.platform.core.id;

import io.forge.platform.core.validation.Validation;
import java.util.UUID;

/**
 * Typed identifier for a decision.
 *
 * @param value the decision UUIDv7 value
 */
public record DecisionId(UUID value) implements TypedId {
  /**
   * Creates a new decision identifier.
   *
   * @return a new decision identifier
   */
  public static DecisionId newId() {
    return of(InternalUuidGenerator.generate());
  }

  /**
   * Creates a decision identifier from an existing UUIDv7 value.
   *
   * @param value the UUIDv7 value
   * @return a decision identifier
   */
  public static DecisionId of(UUID value) {
    return new DecisionId(value);
  }

  public DecisionId {
    Validation.requireNonNull(value, "value must not be null");
    Validation.requireTrue(
        InternalUuidGenerator.isVersion7(value), "DecisionId must wrap a UUIDv7");
  }
}
