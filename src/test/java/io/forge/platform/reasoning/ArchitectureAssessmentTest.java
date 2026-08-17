package io.forge.platform.reasoning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureAssessmentTest {

  @Test
  void preservesFieldsExactly() {
    List<String> evidence = List.of("fact one", "fact two");

    ArchitectureAssessment assessment = new ArchitectureAssessment(evidence, "narrative text");

    assertEquals(evidence, assessment.evidence());
    assertEquals("narrative text", assessment.narrative());
  }

  @Test
  void defensivelyCopiesEvidence() {
    List<String> mutableEvidence = new ArrayList<>(List.of("fact"));

    ArchitectureAssessment assessment = new ArchitectureAssessment(mutableEvidence, "narrative");
    mutableEvidence.add("another fact");

    assertEquals(1, assessment.evidence().size());
    assertThrows(
        UnsupportedOperationException.class, () -> assessment.evidence().add("third fact"));
  }

  @Test
  void rejectsNullEvidence() {
    assertThrows(NullPointerException.class, () -> new ArchitectureAssessment(null, "narrative"));
  }

  @Test
  void rejectsNullNarrative() {
    assertThrows(NullPointerException.class, () -> new ArchitectureAssessment(List.of(), null));
  }
}
