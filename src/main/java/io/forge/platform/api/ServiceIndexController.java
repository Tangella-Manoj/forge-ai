package io.forge.platform.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Answers the root path with a description of the service and its endpoints.
 *
 * <p>Separate from {@link AnalysisController} because it has a different responsibility: that class
 * exposes analysis, this one only describes the surface. It performs no analysis and reads no
 * repository, so it needs no workspace and no path validation.
 */
@RestController
class ServiceIndexController {

  private final String version;

  ServiceIndexController(@Value("${forge.version:unknown}") String version) {
    this.version = version;
  }

  @GetMapping("/")
  ServiceIndexResponse index() {
    return new ServiceIndexResponse(
        "Forge AI Platform",
        version,
        "Evidence-backed engineering intelligence: repository structure, change impact, and risk"
            + " findings derived from a repository's real build and import graph.",
        List.of(
            new ServiceIndexResponse.Endpoint(
                "GET",
                "/api/v1/analysis?repository={path}",
                "Modules, module dependencies, and risk findings. 'repository' is optional and"
                    + " relative to the configured workspace root."),
            new ServiceIndexResponse.Endpoint(
                "GET",
                "/api/v1/impact?module={artifactId}&repository={path}",
                "What else a change to the given module would affect."),
            new ServiceIndexResponse.Endpoint(
                "GET", "/actuator/health", "Liveness and readiness, for deployment probes.")));
  }
}
