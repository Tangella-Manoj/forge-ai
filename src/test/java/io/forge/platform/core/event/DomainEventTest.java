package io.forge.platform.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.forge.platform.core.id.EventId;
import io.forge.platform.core.id.RepositoryId;
import io.forge.platform.core.id.TypedId;
import io.forge.platform.core.id.WorkspaceId;
import io.forge.platform.core.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DomainEventTest {
  private record TestEvent(EventId eventId, Instant occurredAt, TypedId aggregateId, long version)
      implements DomainEvent {}

  @Test
  void exposesRequiredMetadata() {
    EventId eventId = EventId.newId();
    WorkspaceId aggregateId = WorkspaceId.newId();
    Instant occurredAt = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z")).now();

    DomainEvent event = new TestEvent(eventId, occurredAt, aggregateId, 1L);

    assertEquals(eventId, event.eventId());
    assertEquals(occurredAt, event.occurredAt());
    assertEquals(aggregateId, event.aggregateId());
    assertEquals(1L, event.version());
  }

  @Test
  void occurredAtReflectsWhicheverClockProducedIt() {
    Instant fixedInstant = Instant.parse("2020-06-15T12:00:00Z");
    Clock clock = Clock.fixed(fixedInstant);

    DomainEvent event = new TestEvent(EventId.newId(), clock.now(), WorkspaceId.newId(), 1L);

    assertEquals(fixedInstant, event.occurredAt());
  }

  @Test
  void acceptsAnyTypedIdAsAggregateId() {
    DomainEvent workspaceEvent =
        new TestEvent(EventId.newId(), Instant.now(), WorkspaceId.newId(), 1L);
    DomainEvent repositoryEvent =
        new TestEvent(EventId.newId(), Instant.now(), RepositoryId.newId(), 1L);

    assertNotNull(workspaceEvent.aggregateId());
    assertNotNull(repositoryEvent.aggregateId());
  }
}
