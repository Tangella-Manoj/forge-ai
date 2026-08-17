package io.forge.platform.intelligence.risk;

/**
 * The kind of risk a {@link RiskFinding} describes.
 *
 * <p>Each value corresponds to exactly one transparent rule in {@link RiskAnalyzer}. New values are
 * added only when a new rule is implemented — never speculatively.
 */
public enum RiskCategory {

  /** Two or more packages within one module are mutually reachable via imports. */
  CIRCULAR_PACKAGE_DEPENDENCY,

  /** Two or more modules within the workspace are mutually reachable via build dependencies. */
  CIRCULAR_MODULE_DEPENDENCY,

  /** Changing one module affects an unusually large share of the workspace. */
  CHANGE_AMPLIFICATION
}
