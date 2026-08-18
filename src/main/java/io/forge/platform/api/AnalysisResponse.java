package io.forge.platform.api;

import java.util.List;

/**
 * The API's analysis contract.
 *
 * <p>Deliberately distinct from the internal domain records rather than serializing those directly:
 * the wire format is a long-term promise to clients, and coupling it to internal types would mean
 * any refactor inside {@code intelligence.*} silently becomes a breaking API change.
 *
 * @param workspace the analyzed path, relative to the configured workspace root
 * @param modules one entry per module found
 * @param moduleDependencies build-time dependencies between modules in this workspace
 * @param riskFindings risks identified by transparent rules
 */
record AnalysisResponse(
    String workspace,
    List<Module> modules,
    List<ModuleDependencyEdge> moduleDependencies,
    List<Finding> riskFindings) {

  /**
   * One module's structural facts.
   *
   * @param groupId build group identifier
   * @param artifactId build artifact identifier
   * @param version build version
   * @param javaVersion declared Java release version
   * @param packageCount number of packages under the module's main source root
   * @param internalDependencyCount observed package-to-package imports within the module
   */
  record Module(
      String groupId,
      String artifactId,
      String version,
      int javaVersion,
      int packageCount,
      int internalDependencyCount) {}

  /**
   * One build-time dependency between two modules in this workspace.
   *
   * @param from the module declaring the dependency
   * @param to the module depended on
   */
  record ModuleDependencyEdge(String from, String to) {}

  /**
   * One risk finding. Evidence, reason, and recommendation stay separate fields on the wire too — a
   * client must be able to render a measured fact differently from advice.
   *
   * @param category which rule produced this finding
   * @param severity how much attention it warrants
   * @param subject what the finding is about
   * @param evidence the observed facts that triggered the rule
   * @param reason why those facts matter
   * @param recommendation suggested action; advice, not fact
   */
  record Finding(
      String category,
      String severity,
      String subject,
      List<String> evidence,
      String reason,
      String recommendation) {}
}
