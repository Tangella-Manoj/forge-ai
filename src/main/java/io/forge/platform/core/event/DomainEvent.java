package io.forge.platform.core.event;

import io.forge.platform.core.id.EventId;
import io.forge.platform.core.id.TypedId;
import java.time.Instant;

/**
 * Contract for a business-meaningful occurrence within a single aggregate's lifecycle.
 *
 * <p>This is a kernel-level contract only: it carries no binding to Kafka, Spring, or any
 * transport. A future boundary-crossing {@code IntegrationEvent} may translate a {@code
 * DomainEvent} for transport when that need is concrete.
 *
 * <p>{@code occurredAt()} must be produced via {@link io.forge.platform.core.time.Clock}, never a
 * direct time call, so event timestamps stay deterministic and testable.
 */
public interface DomainEvent {
  /**
   * Returns the unique identifier of this event.
   *
   * @return the event identifier
   */
  EventId eventId();

  /**
   * Returns the instant at which this event occurred.
   *
   * @return the occurrence instant
   */
  Instant occurredAt();

  /**
   * Returns the identifier of the aggregate this event happened to.
   *
   * @return the aggregate identifier
   */
  TypedId aggregateId();

  /**
   * Returns the version of the aggregate after this event was applied.
   *
   * @return the aggregate version
   */
  long version();
}
