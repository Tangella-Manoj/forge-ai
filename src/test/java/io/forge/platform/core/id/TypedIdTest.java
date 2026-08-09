package io.forge.platform.core.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TypedIdTest {
  @Test
  void createsDistinctIdentifiers() {
    WorkspaceId first = WorkspaceId.newId();
    WorkspaceId second = WorkspaceId.newId();

    assertNotEquals(first, second);
    assertNotEquals(first.value(), second.value());
  }

  @Test
  void preservesValueSemanticsForEachIdType() {
    UUID uuid = InternalUuidGenerator.generate();

    WorkspaceId workspaceA = WorkspaceId.of(uuid);
    WorkspaceId workspaceB = WorkspaceId.of(uuid);
    RepositoryId repository = RepositoryId.of(InternalUuidGenerator.generate());
    RepositoryId sameUuidRepository = RepositoryId.of(uuid);

    assertEquals(workspaceA, workspaceB);
    assertEquals(workspaceA.hashCode(), workspaceB.hashCode());
    assertNotEquals(workspaceA, repository);
    assertNotEquals(workspaceA, sameUuidRepository);
    assertEquals("WorkspaceId[value=" + uuid + "]", workspaceA.toString());
  }

  @Test
  void preservesFactoryValuesExactly() {
    UUID uuid = InternalUuidGenerator.generate();

    assertEquals(uuid, WorkspaceId.of(uuid).value());
    assertEquals(uuid, RepositoryId.of(uuid).value());
    assertEquals(uuid, DecisionId.of(uuid).value());
    assertEquals(uuid, AnalysisId.of(uuid).value());
    assertEquals(uuid, EventId.of(uuid).value());
  }

  @Test
  void rejectsNullValues() {
    assertThrows(NullPointerException.class, () -> WorkspaceId.of(null));
    assertThrows(NullPointerException.class, () -> RepositoryId.of(null));
    assertThrows(NullPointerException.class, () -> DecisionId.of(null));
    assertThrows(NullPointerException.class, () -> AnalysisId.of(null));
    assertThrows(NullPointerException.class, () -> EventId.of(null));
  }

  @Test
  void rejectsNonVersion7Uuids() {
    UUID v4 = UUID.randomUUID();

    assertThrows(IllegalArgumentException.class, () -> WorkspaceId.of(v4));
    assertThrows(IllegalArgumentException.class, () -> RepositoryId.of(v4));
    assertThrows(IllegalArgumentException.class, () -> DecisionId.of(v4));
    assertThrows(IllegalArgumentException.class, () -> AnalysisId.of(v4));
    assertThrows(IllegalArgumentException.class, () -> EventId.of(v4));
  }

  @Test
  void generatedIdsAreVersion7Uuids() {
    WorkspaceId workspaceId = WorkspaceId.newId();
    RepositoryId repositoryId = RepositoryId.newId();
    DecisionId decisionId = DecisionId.newId();
    AnalysisId analysisId = AnalysisId.newId();
    EventId eventId = EventId.newId();

    assertEquals(7, workspaceId.value().version());
    assertEquals(7, repositoryId.value().version());
    assertEquals(7, decisionId.value().version());
    assertEquals(7, analysisId.value().version());
    assertEquals(7, eventId.value().version());
    assertTrue(workspaceId.value().variant() == 2);
    assertTrue(repositoryId.value().variant() == 2);
    assertTrue(decisionId.value().variant() == 2);
    assertTrue(analysisId.value().variant() == 2);
    assertTrue(eventId.value().variant() == 2);
  }
}
