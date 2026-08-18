package io.forge.platform.api;

import io.forge.platform.core.error.PlatformError;
import io.forge.platform.intelligence.change.ChangeImpact;
import io.forge.platform.intelligence.change.ChangeImpactAnalyzer;
import io.forge.platform.intelligence.model.EngineeringModel;
import io.forge.platform.intelligence.model.EngineeringModelBuilder;
import io.forge.platform.intelligence.model.ModuleDependency;
import io.forge.platform.intelligence.repository.RepositoryScanner;
import io.forge.platform.intelligence.repository.RepositorySnapshot;
import io.forge.platform.intelligence.risk.RiskAnalyzer;
import io.forge.platform.intelligence.risk.RiskFinding;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes Forge's existing analysis capabilities over HTTP. Deliberately thin: it resolves and
 * validates the requested path, delegates to the same {@code intelligence.*} code the CLI uses, and
 * maps the result onto the API's own DTOs. No analysis logic lives here.
 *
 * <p>Errors are RFC 9457 Problem Details (via {@link ProblemDetail}) rather than ad-hoc JSON, per
 * the project's engineering standards. A {@link PlatformError}'s stable machine-readable code is
 * carried as an extension property so clients can branch on it without parsing prose.
 */
@RestController
@RequestMapping("/api/v1")
class AnalysisController {

  private final WorkspacePathResolver pathResolver;

  AnalysisController(WorkspacePathResolver pathResolver) {
    this.pathResolver = pathResolver;
  }

  /**
   * Scans a repository and returns its structure, module dependencies, and risk findings.
   *
   * @param repository path relative to the configured workspace root; defaults to the root itself
   * @return the analysis, or a Problem Detail
   */
  @GetMapping("/analysis")
  ResponseEntity<?> analyze(
      @RequestParam(name = "repository", required = false) String repository) {
    return pathResolver
        .resolve(repository)
        .fold(
            target ->
                RepositoryScanner.scanWorkspace(target)
                    .fold(
                        snapshots -> ResponseEntity.ok(toAnalysisResponse(target, snapshots)),
                        error -> problem(error, HttpStatus.BAD_REQUEST)),
            error -> problem(error, HttpStatus.BAD_REQUEST));
  }

  /**
   * Reports what else in the workspace a change to {@code module} would affect.
   *
   * @param module the artifactId being changed
   * @param repository path relative to the configured workspace root; defaults to the root itself
   * @return the impact, or a Problem Detail
   */
  @GetMapping("/impact")
  ResponseEntity<?> impact(
      @RequestParam(name = "module") String module,
      @RequestParam(name = "repository", required = false) String repository) {
    return pathResolver
        .resolve(repository)
        .fold(
            target ->
                RepositoryScanner.scanWorkspace(target)
                    .fold(
                        snapshots ->
                            ChangeImpactAnalyzer.analyze(
                                    EngineeringModelBuilder.build(snapshots), module)
                                .fold(
                                    impact -> ResponseEntity.ok(toImpactResponse(target, impact)),
                                    error -> problem(error, HttpStatus.NOT_FOUND)),
                        error -> problem(error, HttpStatus.BAD_REQUEST)),
            error -> problem(error, HttpStatus.BAD_REQUEST));
  }

  private AnalysisResponse toAnalysisResponse(Path target, List<RepositorySnapshot> snapshots) {
    EngineeringModel model = EngineeringModelBuilder.build(snapshots);

    List<AnalysisResponse.Module> modules =
        snapshots.stream()
            .map(
                snapshot ->
                    new AnalysisResponse.Module(
                        snapshot.coordinates().groupId(),
                        snapshot.coordinates().artifactId(),
                        snapshot.coordinates().version(),
                        snapshot.javaVersion(),
                        snapshot.packages().size(),
                        snapshot.internalDependencies().size()))
            .toList();

    List<AnalysisResponse.ModuleDependencyEdge> edges =
        model.moduleDependencies().stream()
            .sorted(
                Comparator.comparing(ModuleDependency::fromModule)
                    .thenComparing(ModuleDependency::toModule))
            .map(
                dependency ->
                    new AnalysisResponse.ModuleDependencyEdge(
                        dependency.fromModule(), dependency.toModule()))
            .toList();

    List<AnalysisResponse.Finding> findings =
        RiskAnalyzer.analyze(model).stream().map(AnalysisController::toFinding).toList();

    return new AnalysisResponse(pathResolver.relativize(target), modules, edges, findings);
  }

  private static AnalysisResponse.Finding toFinding(RiskFinding finding) {
    return new AnalysisResponse.Finding(
        finding.category().name(),
        finding.severity().name(),
        finding.subject(),
        finding.evidence(),
        finding.reason(),
        finding.recommendation());
  }

  private ImpactResponse toImpactResponse(Path target, ChangeImpact impact) {
    return new ImpactResponse(
        pathResolver.relativize(target),
        impact.changedModule(),
        impact.directDependents().stream().sorted().toList(),
        impact.transitiveDependents().stream().sorted().toList(),
        impact.affectedModuleCount(),
        ImpactResponse.BUILD_TIME_SCOPE);
  }

  private static ResponseEntity<ProblemDetail> problem(PlatformError error, HttpStatus status) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, error.message());
    detail.setTitle("Analysis failed");
    detail.setProperty("code", error.code());
    return ResponseEntity.status(status).body(detail);
  }
}
