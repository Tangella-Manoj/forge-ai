package io.forge.platform.intelligence.repository;

import io.forge.platform.core.error.InfrastructureError;
import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import io.forge.platform.core.validation.Validation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Builds a {@link RepositorySnapshot} by reading a single Maven module's {@code pom.xml} and
 * walking its {@code src/main/java} tree.
 *
 * <p>Deliberately scoped to one module per call — a caller wanting a multi-module view calls this
 * once per child module and composes the results, rather than this class recursing through {@code
 * <modules>} declarations it cannot yet validate against a real multi-module fixture.
 *
 * <p>All failures (missing/unreadable {@code pom.xml}, malformed XML, missing required fields) are
 * expected, recoverable outcomes — returned as a {@link Result}, never thrown.
 */
public final class RepositoryScanner {

  private RepositoryScanner() {}

  /**
   * Scans a module rooted at {@code moduleRoot}.
   *
   * @param moduleRoot the module's root directory, expected to contain {@code pom.xml} and {@code
   *     src/main/java}
   * @return the module's snapshot, or a failure describing why it could not be built
   */
  public static Result<RepositorySnapshot, PlatformError> scan(Path moduleRoot) {
    Validation.requireNonNull(moduleRoot, "moduleRoot must not be null");

    Path pomPath = moduleRoot.resolve("pom.xml");
    if (!Files.isRegularFile(pomPath)) {
      return Result.failure(
          InfrastructureError.of("repository.scan.pom_missing", "No pom.xml found at " + pomPath));
    }

    Element project;
    try {
      project = parsePomXml(pomPath);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      return Result.failure(
          InfrastructureError.of(
              "repository.scan.pom_unparseable", "Could not parse " + pomPath, e));
    }

    String groupId = childText(project, "groupId");
    String artifactId = childText(project, "artifactId");
    String version = childText(project, "version");
    if (groupId == null || artifactId == null || version == null) {
      return Result.failure(
          InfrastructureError.of(
              "repository.scan.coordinates_missing",
              "pom.xml is missing a top-level groupId, artifactId, or version at " + pomPath));
    }

    Integer javaVersion = readJavaVersion(project);
    if (javaVersion == null) {
      return Result.failure(
          InfrastructureError.of(
              "repository.scan.java_version_missing",
              "pom.xml declares neither maven.compiler.release nor java.version at " + pomPath));
    }

    Path sourceRoot = moduleRoot.resolve("src").resolve("main").resolve("java");
    SourceScanResult sourceScan;
    try {
      sourceScan = scanSourceTree(sourceRoot);
    } catch (IOException e) {
      return Result.failure(
          InfrastructureError.of(
              "repository.scan.source_unreadable",
              "Could not walk source tree at " + sourceRoot,
              e));
    }

    return Result.success(
        new RepositorySnapshot(
            new BuildCoordinates(groupId, artifactId, version),
            javaVersion,
            sourceScan.packages(),
            sourceScan.internalDependencies()));
  }

  private static Element parsePomXml(Path pomPath)
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    // Malformed pom.xml is an expected, handled outcome (see the catch in scan()) — the default
    // handler logs it to stderr as "[Fatal Error]" regardless, which misleadingly looks like an
    // unhandled failure in CI output. Silence it; the SAXException still propagates normally.
    builder.setErrorHandler(
        new ErrorHandler() {
          @Override
          public void warning(SAXParseException exception) {}

          @Override
          public void error(SAXParseException exception) {}

          @Override
          public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
          }
        });
    Document document = builder.parse(pomPath.toFile());
    return document.getDocumentElement();
  }

  private static String childText(Element parent, String tagName) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i) instanceof Element element && element.getTagName().equals(tagName)) {
        return element.getTextContent().trim();
      }
    }
    return null;
  }

  private static Integer readJavaVersion(Element project) {
    NodeList propertiesNodes = project.getElementsByTagName("properties");
    if (propertiesNodes.getLength() == 0) {
      return null;
    }
    Element properties = (Element) propertiesNodes.item(0);
    for (String tag : List.of("maven.compiler.release", "java.version")) {
      String value = childText(properties, tag);
      if (value != null) {
        try {
          return Integer.parseInt(value);
        } catch (NumberFormatException e) {
          return null;
        }
      }
    }
    return null;
  }

  /**
   * Walks {@code sourceRoot} once, collecting both per-package class counts and raw {@code import}
   * lines, then resolves those imports against the now-complete package set — an import can only be
   * recognized as internal once every package in the module is known.
   */
  private static SourceScanResult scanSourceTree(Path sourceRoot) throws IOException {
    if (!Files.isDirectory(sourceRoot)) {
      return new SourceScanResult(List.of(), Set.of());
    }

    Map<String, Integer> classCountByPackage = new LinkedHashMap<>();
    List<ImportStatement> imports = new java.util.ArrayList<>();

    try (Stream<Path> files = Files.walk(sourceRoot)) {
      for (Path path :
          files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).toList()) {
        Path relativeDir = sourceRoot.relativize(path.getParent());
        String packageName = relativeDir.toString().replace(java.io.File.separatorChar, '.');
        classCountByPackage.merge(packageName, 1, Integer::sum);
        imports.addAll(parseImports(path, packageName));
      }
    }

    Set<String> knownPackages = classCountByPackage.keySet();
    Set<PackageDependency> dependencies = new LinkedHashSet<>();
    for (ImportStatement statement : imports) {
      String candidate = withoutLastSegment(statement.importedFqcn());
      if (!knownPackages.contains(candidate) && statement.isStatic()) {
        candidate = withoutLastSegment(candidate);
      }
      if (knownPackages.contains(candidate) && !candidate.equals(statement.fromPackage())) {
        dependencies.add(new PackageDependency(statement.fromPackage(), candidate));
      }
    }

    List<PackageSummary> packages =
        classCountByPackage.entrySet().stream()
            .map(entry -> new PackageSummary(entry.getKey(), entry.getValue()))
            .toList();

    return new SourceScanResult(packages, dependencies);
  }

  private static List<ImportStatement> parseImports(Path javaFile, String fromPackage)
      throws IOException {
    List<ImportStatement> imports = new java.util.ArrayList<>();
    for (String line : Files.readAllLines(javaFile)) {
      String trimmed = line.strip();
      if (!trimmed.startsWith("import ")) {
        continue;
      }
      String rest = trimmed.substring("import ".length()).strip();
      boolean isStatic = rest.startsWith("static ");
      if (isStatic) {
        rest = rest.substring("static ".length()).strip();
      }
      if (rest.endsWith(";")) {
        rest = rest.substring(0, rest.length() - 1);
      }
      imports.add(new ImportStatement(fromPackage, rest, isStatic));
    }
    return imports;
  }

  private static String withoutLastSegment(String fullyQualifiedName) {
    int lastDot = fullyQualifiedName.lastIndexOf('.');
    return lastDot < 0 ? "" : fullyQualifiedName.substring(0, lastDot);
  }

  private record ImportStatement(String fromPackage, String importedFqcn, boolean isStatic) {}

  private record SourceScanResult(
      List<PackageSummary> packages, Set<PackageDependency> internalDependencies) {}
}
