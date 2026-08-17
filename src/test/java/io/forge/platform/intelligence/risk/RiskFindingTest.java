package io.forge.platform.intelligence.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiskFindingTest {

  private static RiskFinding finding(List<String> evidence) {
    return new RiskFinding(
        RiskCategory.CHANGE_AMPLIFICATION,
        RiskSeverity.MEDIUM,
        "common",
        evidence,
        "reason",
        "recommendation");
  }

  @Test
  void preservesFieldsExactly() {
    RiskFinding f = finding(List.of("fact"));

    assertEquals(RiskCategory.CHANGE_AMPLIFICATION, f.category());
    assertEquals(RiskSeverity.MEDIUM, f.severity());
    assertEquals("common", f.subject());
    assertEquals(List.of("fact"), f.evidence());
    assertEquals("reason", f.reason());
    assertEquals("recommendation", f.recommendation());
  }

  @Test
  void defensivelyCopiesEvidence() {
    List<String> mutable = new ArrayList<>(List.of("fact"));

    RiskFinding f = finding(mutable);
    mutable.add("another");

    assertEquals(1, f.evidence().size());
    assertThrows(UnsupportedOperationException.class, () -> f.evidence().add("third"));
  }

  @Test
  void rejectsEmptyEvidence() {
    // A finding with no evidence is an assertion, not an analysis — the whole point is that a
    // reader can check the claim.
    assertThrows(IllegalArgumentException.class, () -> finding(List.of()));
  }

  @Test
  void rejectsBlankTextFields() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RiskFinding(
                RiskCategory.CHANGE_AMPLIFICATION,
                RiskSeverity.MEDIUM,
                "  ",
                List.of("fact"),
                "reason",
                "recommendation"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RiskFinding(
                RiskCategory.CHANGE_AMPLIFICATION,
                RiskSeverity.MEDIUM,
                "common",
                List.of("fact"),
                "  ",
                "recommendation"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RiskFinding(
                RiskCategory.CHANGE_AMPLIFICATION,
                RiskSeverity.MEDIUM,
                "common",
                List.of("fact"),
                "reason",
                "  "));
  }

  @Test
  void rejectsNullFields() {
    assertThrows(
        NullPointerException.class,
        () ->
            new RiskFinding(
                null, RiskSeverity.MEDIUM, "common", List.of("f"), "reason", "recommendation"));
    assertThrows(
        NullPointerException.class,
        () ->
            new RiskFinding(
                RiskCategory.CHANGE_AMPLIFICATION,
                null,
                "common",
                List.of("f"),
                "reason",
                "recommendation"));
    assertThrows(
        NullPointerException.class,
        () ->
            new RiskFinding(
                RiskCategory.CHANGE_AMPLIFICATION,
                RiskSeverity.MEDIUM,
                "common",
                null,
                "reason",
                "recommendation"));
  }
}
