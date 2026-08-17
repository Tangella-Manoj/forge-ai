package io.forge.platform.intelligence.risk;

/**
 * How much attention a {@link RiskFinding} warrants.
 *
 * <p>Only two levels exist, deliberately: every rule producing findings today maps cleanly to one
 * of them, and inventing unused levels would imply a precision the transparent, rule-based analysis
 * does not have.
 */
public enum RiskSeverity {

  /**
   * An objective defect — something that is wrong regardless of intent, such as a circular
   * dependency that prevents clean layering.
   */
  HIGH,

  /**
   * A cost or caution signal, not necessarily a defect — something that may be entirely intentional
   * but is worth knowing before making a change.
   */
  MEDIUM
}
