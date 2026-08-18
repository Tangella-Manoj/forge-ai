package io.forge.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Pins the root path's behavior. This exists because the deployed service originally answered "/"
 * with a bare 404, which reads as a broken deployment even though every real endpoint was healthy —
 * a regression here is invisible in logs and metrics but highly visible to anyone opening the URL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServiceIndexControllerTest {

  @LocalServerPort private int port;

  private JsonNode index() {
    return RestClient.create()
        .get()
        .uri("http://localhost:" + port + "/")
        .retrieve()
        .body(JsonNode.class);
  }

  @Test
  void rootDescribesTheServiceRatherThanReturningNotFound() {
    JsonNode body = index();

    assertEquals("Forge AI Platform", body.get("service").asString());
    assertFalse(body.get("description").asString().isBlank());
  }

  @Test
  void rootReportsTheRunningBuildVersion() {
    String version = index().get("version").asString();

    assertFalse(
        version.contains("@"), "Maven resource filtering did not replace the version placeholder");
    assertNotEquals(
        "unknown", version, "fell back to the default, so forge.version was never configured");
  }

  @Test
  void rootListsEveryCallableEndpoint() {
    JsonNode endpoints = index().get("endpoints");

    assertEquals(3, endpoints.size());
    for (JsonNode endpoint : endpoints) {
      assertFalse(endpoint.get("method").asString().isBlank());
      assertTrue(endpoint.get("path").asString().startsWith("/"));
      assertFalse(endpoint.get("purpose").asString().isBlank());
    }
  }
}
