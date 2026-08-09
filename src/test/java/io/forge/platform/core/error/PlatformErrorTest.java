package io.forge.platform.core.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlatformErrorTest {
  @Test
  void createsErrorWithNoDetailsAndNoCause() {
    DomainError error = DomainError.of("workspace.name.blank", "Workspace name must not be blank");

    assertEquals("workspace.name.blank", error.code());
    assertEquals("Workspace name must not be blank", error.message());
    assertTrue(error.details().isEmpty());
    assertTrue(error.cause().isEmpty());
  }

  @Test
  void createsErrorWithDetails() {
    Map<String, String> details = Map.of("field", "name");
    DomainError error =
        DomainError.of("workspace.name.blank", "Workspace name must not be blank", details);

    assertEquals(details, error.details());
    assertTrue(error.cause().isEmpty());
  }

  @Test
  void createsErrorWithCause() {
    RuntimeException cause = new RuntimeException("boom");
    InfrastructureError error =
        InfrastructureError.of("database.connection.failed", "Could not reach the database", cause);

    assertTrue(error.details().isEmpty());
    assertTrue(error.cause().isPresent());
    assertEquals(cause, error.cause().orElseThrow());
  }

  @Test
  void createsErrorWithDetailsAndCause() {
    Map<String, String> details = Map.of("host", "db-primary");
    RuntimeException cause = new RuntimeException("boom");
    InfrastructureError error =
        InfrastructureError.of(
            "database.connection.failed", "Could not reach the database", details, cause);

    assertEquals(details, error.details());
    assertEquals(cause, error.cause().orElseThrow());
  }

  @Test
  void treatsNullCauseAsEmptyOptional() {
    DomainError error =
        DomainError.of(
            "workspace.name.blank", "Workspace name must not be blank", (Throwable) null);

    assertTrue(error.cause().isEmpty());
  }

  @Test
  void detailsAreDefensivelyCopiedAndImmutable() {
    Map<String, String> mutableDetails = new HashMap<>();
    mutableDetails.put("field", "name");

    DomainError error =
        DomainError.of("workspace.name.blank", "Workspace name must not be blank", mutableDetails);
    mutableDetails.put("field", "mutated");

    assertEquals(Map.of("field", "name"), error.details());
    assertThrows(UnsupportedOperationException.class, () -> error.details().put("field", "other"));
  }

  @Test
  void rejectsNullCode() {
    assertThrows(
        NullPointerException.class, () -> DomainError.of(null, "Workspace name must not be blank"));
  }

  @Test
  void rejectsBlankCode() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DomainError.of("  ", "Workspace name must not be blank"));
  }

  @Test
  void rejectsNullMessage() {
    assertThrows(NullPointerException.class, () -> DomainError.of("workspace.name.blank", null));
  }

  @Test
  void rejectsBlankMessage() {
    assertThrows(
        IllegalArgumentException.class, () -> DomainError.of("workspace.name.blank", "  "));
  }

  @Test
  void rejectsNullDetails() {
    assertThrows(
        NullPointerException.class,
        () ->
            DomainError.of(
                "workspace.name.blank",
                "Workspace name must not be blank",
                (Map<String, String>) null));
  }

  @Test
  void preservesValueSemanticsWithinTheSameErrorType() {
    DomainError first = DomainError.of("workspace.name.blank", "Workspace name must not be blank");
    DomainError second = DomainError.of("workspace.name.blank", "Workspace name must not be blank");

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  void domainAndInfrastructureErrorsAreNeverEqualEvenWithIdenticalFields() {
    DomainError domainError = DomainError.of("shared.code", "Shared message");
    InfrastructureError infrastructureError =
        InfrastructureError.of("shared.code", "Shared message");

    assertNotEquals(domainError, infrastructureError);
  }

  @Test
  void toStringIsHumanReadable() {
    DomainError error = DomainError.of("workspace.name.blank", "Workspace name must not be blank");

    assertTrue(error.toString().contains("workspace.name.blank"));
    assertTrue(error.toString().contains("Workspace name must not be blank"));
  }

  @Test
  void bothErrorTypesImplementThePlatformErrorContract() {
    PlatformError domainError =
        DomainError.of("workspace.name.blank", "Workspace name must not be blank");
    PlatformError infrastructureError =
        InfrastructureError.of("database.connection.failed", "Could not reach the database");

    assertFalse(domainError instanceof InfrastructureError);
    assertFalse(infrastructureError instanceof DomainError);
  }
}
