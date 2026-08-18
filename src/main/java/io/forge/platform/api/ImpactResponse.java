package io.forge.platform.api;

import java.util.List;

/**
 * The API's change-impact contract.
 *
 * @param workspace the analyzed path, relative to the configured workspace root
 * @param changedModule the module the caller asked about
 * @param directDependents modules declaring a direct dependency on it
 * @param transitiveDependents modules affected only through an intermediate module
 * @param affectedModuleCount direct plus transitive
 * @param scope the analysis's stated limit, so a client cannot present the numbers as more than
 *     they are
 */
record ImpactResponse(
    String workspace,
    String changedModule,
    List<String> directDependents,
    List<String> transitiveDependents,
    int affectedModuleCount,
    String scope) {

  static final String BUILD_TIME_SCOPE =
      "Build-time (Maven dependency) coupling only. Services calling each other over HTTP without"
          + " a declared dependency are not represented.";
}
