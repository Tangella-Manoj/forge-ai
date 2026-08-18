package io.forge.platform.api;

import java.util.List;

/**
 * What the service is and what it exposes.
 *
 * <p>Exists because the root path is the first thing anyone opens, and a bare 404 there reads as a
 * broken deployment even when every real endpoint is healthy.
 *
 * @param service the application name
 * @param version the running build's version
 * @param description one line on what this service does
 * @param endpoints the callable endpoints, each with its purpose
 */
record ServiceIndexResponse(
    String service, String version, String description, List<Endpoint> endpoints) {

  /**
   * One callable endpoint.
   *
   * @param method HTTP method
   * @param path path, including any query parameters that matter
   * @param purpose what it returns
   */
  record Endpoint(String method, String path, String purpose) {}
}
