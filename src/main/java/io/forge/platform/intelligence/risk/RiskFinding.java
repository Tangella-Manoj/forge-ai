package io.forge.platform.intelligence.risk;

import io.forge.platform.core.validation.Validation;
import java.util.List;

/**
 * One risk identified by a transparent, explainable rule.
 *
 * <p>{@code evidence} and {@code recommendation} are deliberately separate fields, never merged
 * into one narrative: evidence is an observed fact the analyzer measured, while a recommendation is
 * advice a human is free to reject. A reader must always be able to tell which is which — the same
 * fact/inference separation {@link io.forge.platform.reasoning.ArchitectureAssessment} maintains
 * between deterministic evidence and AI narrative.
 *
 * @param category which rule produced this finding
 * @param severity how much attention it warrants
 * @param subject what the finding is about — a module artifactId, typically
 * @param evidence the observed facts that triggered the rule, each independently verifiable
 * @param reason why those facts matter, in engineering terms
 * @param recommendation suggested action; advice, not a fact
 */
public record RiskFinding(
    RiskCategory category,
    RiskSeverity severity,
    String subject,
    List<String> evidence,
    String reason,
    String recommendation) {

  public RiskFinding {
    Validation.requireNonNull(category, "category must not be null");
    Validation.requireNonNull(severity, "severity must not be null");
    Validation.requireNonBlank(subject, "subject must not be blank");
    Validation.requireNonNull(evidence, "evidence must not be null");
    Validation.requireTrue(!evidence.isEmpty(), "evidence must not be empty");
    Validation.requireNonBlank(reason, "reason must not be blank");
    Validation.requireNonBlank(recommendation, "recommendation must not be blank");
    evidence = List.copyOf(evidence);
  }
}
