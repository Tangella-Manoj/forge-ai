package io.forge.platform.intelligence.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BuildCoordinatesTest {

  @Test
  void preservesFieldsExactly() {
    BuildCoordinates coordinates = new BuildCoordinates("io.forge.platform", "forge-ai", "0.1.0");

    assertEquals("io.forge.platform", coordinates.groupId());
    assertEquals("forge-ai", coordinates.artifactId());
    assertEquals("0.1.0", coordinates.version());
  }

  @Test
  void rejectsBlankGroupId() {
    assertThrows(
        IllegalArgumentException.class, () -> new BuildCoordinates("  ", "forge-ai", "0.1.0"));
  }

  @Test
  void rejectsBlankArtifactId() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BuildCoordinates("io.forge.platform", "  ", "0.1.0"));
  }

  @Test
  void rejectsBlankVersion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BuildCoordinates("io.forge.platform", "forge-ai", "  "));
  }

  @Test
  void rejectsNullFields() {
    assertThrows(NullPointerException.class, () -> new BuildCoordinates(null, "forge-ai", "0.1.0"));
    assertThrows(
        NullPointerException.class, () -> new BuildCoordinates("io.forge.platform", null, "0.1.0"));
    assertThrows(
        NullPointerException.class,
        () -> new BuildCoordinates("io.forge.platform", "forge-ai", null));
  }
}
