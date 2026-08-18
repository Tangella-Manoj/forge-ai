package io.forge.platform.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * End-to-end API tests against a real embedded server and a real synthetic workspace, exercising
 * the whole chain an endpoint runs: resolve path → scan → build model → analyze risk → serialize.
 *
 * <p>Uses a real HTTP client rather than a mocked servlet layer deliberately — it also verifies
 * status codes, content negotiation, and JSON serialization as a caller would actually see them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnalysisControllerTest {

  @TempDir static Path workspaceRoot;

  @LocalServerPort private int port;

  @DynamicPropertySource
  static void workspaceRoot(DynamicPropertyRegistry registry) {
    registry.add("forge.workspace.root", () -> workspaceRoot.toString());
  }

  /** common <- loan-service <- gateway, plus a real package cycle inside loan-service. */
  @BeforeAll
  static void writeWorkspace() throws IOException {
    Files.writeString(
        workspaceRoot.resolve("pom.xml"),
        """
        <?xml version="1.0"?>
        <project>
          <groupId>com.example</groupId>
          <artifactId>workspace-parent</artifactId>
          <version>1.0.0</version>
          <packaging>pom</packaging>
          <modules>
            <module>common</module>
            <module>loan-service</module>
            <module>gateway</module>
          </modules>
          <properties>
            <maven.compiler.release>21</maven.compiler.release>
          </properties>
        </project>
        """);
    writeModule("common", null);
    writeModule("loan-service", "common");
    writeModule("gateway", "loan-service");

    Path commandDir =
        Files.createDirectories(
            workspaceRoot.resolve("loan-service/src/main/java/com/example/command"));
    Path sagaDir =
        Files.createDirectories(
            workspaceRoot.resolve("loan-service/src/main/java/com/example/saga"));
    Files.writeString(
        commandDir.resolve("Cmd.java"),
        "package com.example.command;\nimport com.example.saga.Saga;\nclass Cmd {}\n");
    Files.writeString(
        sagaDir.resolve("Saga.java"),
        "package com.example.saga;\nimport com.example.command.Cmd;\nclass Saga {}\n");

    Files.createDirectories(workspaceRoot.resolve("not-a-module"));
  }

  private static void writeModule(String artifactId, String dependsOn) throws IOException {
    Path moduleDir = Files.createDirectories(workspaceRoot.resolve(artifactId));
    String dependencies =
        dependsOn == null
            ? ""
            : "<dependencies><dependency><groupId>com.example</groupId><artifactId>"
                + dependsOn
                + "</artifactId></dependency></dependencies>";
    Files.writeString(
        moduleDir.resolve("pom.xml"),
        "<?xml version=\"1.0\"?><project>"
            + "<parent><groupId>com.example</groupId><artifactId>workspace-parent</artifactId>"
            + "<version>1.0.0</version></parent>"
            + "<artifactId>"
            + artifactId
            + "</artifactId>"
            + dependencies
            + "</project>");
  }

  private record Response(HttpStatusCode status, JsonNode body) {}

  private Response get(String uri) {
    return RestClient.create()
        .get()
        .uri("http://localhost:" + port + uri)
        .exchange(
            (request, response) ->
                new Response(response.getStatusCode(), response.bodyTo(JsonNode.class)),
            false);
  }

  @Test
  void analysisReturnsModulesDependenciesAndRiskFindings() {
    Response response = get("/api/v1/analysis");

    assertTrue(response.status().is2xxSuccessful());
    JsonNode body = response.body();
    assertEquals(".", body.get("workspace").asString());
    assertEquals(4, body.get("modules").size());
    assertEquals(2, body.get("moduleDependencies").size());

    JsonNode topFinding = body.get("riskFindings").get(0);
    assertEquals("HIGH", topFinding.get("severity").asString());
    assertEquals("CIRCULAR_PACKAGE_DEPENDENCY", topFinding.get("category").asString());
    assertEquals("loan-service", topFinding.get("subject").asString());
    assertTrue(topFinding.get("evidence").isArray());
    assertFalse(topFinding.get("recommendation").asString().isBlank());
  }

  @Test
  void analysisCanTargetASingleModuleBeneathTheRoot() {
    Response response = get("/api/v1/analysis?repository=common");

    assertTrue(response.status().is2xxSuccessful());
    assertEquals("common", response.body().get("workspace").asString());
    assertEquals(1, response.body().get("modules").size());
    assertEquals("common", response.body().get("modules").get(0).get("artifactId").asString());
  }

  @Test
  void analysisRejectsPathTraversalWithAProblemDetail() {
    Response response = get("/api/v1/analysis?repository=../../etc");

    assertEquals(400, response.status().value());
    assertEquals("workspace.path_outside_root", response.body().get("code").asString());
    assertEquals("Analysis failed", response.body().get("title").asString());
  }

  @Test
  void analysisRejectsAnAbsolutePath() {
    Response response = get("/api/v1/analysis?repository=/etc");

    assertEquals(400, response.status().value());
    assertEquals("workspace.path_must_be_relative", response.body().get("code").asString());
  }

  @Test
  void analysisReturnsAProblemDetailForAnUnscannableDirectory() {
    Response response = get("/api/v1/analysis?repository=not-a-module");

    assertEquals(400, response.status().value());
    assertEquals("repository.scan.pom_missing", response.body().get("code").asString());
  }

  @Test
  void impactReportsDirectAndTransitiveDependents() {
    Response response = get("/api/v1/impact?module=common");

    assertTrue(response.status().is2xxSuccessful());
    JsonNode body = response.body();
    assertEquals("common", body.get("changedModule").asString());
    assertEquals(2, body.get("affectedModuleCount").asInt());
    assertEquals("loan-service", body.get("directDependents").get(0).asString());
    assertEquals("gateway", body.get("transitiveDependents").get(0).asString());
    assertFalse(body.get("scope").asString().isBlank());
  }

  @Test
  void impactReturnsNotFoundForAnUnknownModule() {
    Response response = get("/api/v1/impact?module=no-such-module");

    assertEquals(404, response.status().value());
    assertEquals("change.module_not_in_workspace", response.body().get("code").asString());
  }

  @Test
  void impactRequiresTheModuleParameter() {
    assertEquals(400, get("/api/v1/impact").status().value());
  }

  @Test
  void healthEndpointIsAvailableForDeploymentProbes() {
    Response response = get("/actuator/health");

    assertTrue(response.status().is2xxSuccessful());
    assertEquals("UP", response.body().get("status").asString());
  }

  @Test
  void sensitiveActuatorEndpointsAreNotExposed() {
    // Security regression guard: env/beans/configprops disclose configuration and internals.
    assertEquals(404, get("/actuator/env").status().value());
    assertEquals(404, get("/actuator/beans").status().value());
    assertEquals(404, get("/actuator/configprops").status().value());
  }
}
