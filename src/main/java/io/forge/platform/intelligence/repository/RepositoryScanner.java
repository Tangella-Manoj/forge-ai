package io.forge.platform.intelligence.repository;

import io.forge.platform.core.error.InfrastructureError;
import io.forge.platform.core.error.PlatformError;
import io.forge.platform.core.result.Result;
import io.forge.platform.core.validation.Validation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
 * walking its {@code src/main/java} tree — {@link #scan(Path)} — or scans every module of a
 * multi-module project via its parent's {@code <modules>} declaration — {@link
 * #scanWorkspace(Path)}.
 *
 * <p>Handles the common real-world case of Maven coordinate/property inheritance (a child module
 * declaring only its own {@code artifactId} and inheriting {@code groupId}/{@code version}/{@code
 * java.version} from a local {@code <parent>}) — verified against a real multi-module project, not
 * assumed correct from the Maven specification alone.
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

    // groupId/version may be inherited from <parent> rather than declared directly — the norm for
    // a properly-structured multi-module project (verified against a real one: DLMP's child
    // modules declare only artifactId). artifactId is never inherited — it is each module's own
    // identity by Maven convention.
    String groupId = resolveInheritedCoordinate(project, "groupId");
    String artifactId = childText(project, "artifactId");
    String version = resolveInheritedCoordinate(project, "version");
    if (groupId == null || artifactId == null || version == null) {
      return Result.failure(
          InfrastructureError.of(
              "repository.scan.coordinates_missing",
              "pom.xml (and its <parent>, if any) is missing a groupId, artifactId, or version at "
                  + pomPath));
    }

    Integer javaVersion = readJavaVersion(project, moduleRoot);
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
            sourceScan.internalDependencies(),
            readDeclaredDependencyArtifactIds(project)));
  }

  private static Set<String> readDeclaredDependencyArtifactIds(Element project) {
    // Deliberately NOT getElementsByTagName("dependencies") — that searches the whole document,
    // and a <dependencyManagement><dependencies> block (version pins, not real applied
    // dependencies) would be found first if it appears earlier in the file than a real top-level
    // <dependencies> block. Verified against a real pom.xml with exactly this shape (a parent POM
    // with only a dependencyManagement block, no top-level dependencies at all) before relying on
    // getElementsByTagName here — it would have silently produced wrong facts. Only <project>'s
    // own direct <dependencies> child is the module's real, applied dependency list.
    Element dependenciesElement = directChild(project, "dependencies");
    if (dependenciesElement == null) {
      return Set.of();
    }

    Set<String> artifactIds = new LinkedHashSet<>();
    NodeList children = dependenciesElement.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i) instanceof Element dependency
          && dependency.getTagName().equals("dependency")) {
        String artifactId = childText(dependency, "artifactId");
        if (artifactId != null) {
          artifactIds.add(artifactId);
        }
      }
    }
    return artifactIds;
  }

  private static Element directChild(Element parent, String tagName) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i) instanceof Element element && element.getTagName().equals(tagName)) {
        return element;
      }
    }
    return null;
  }

  /**
   * Scans a multi-module project rooted at {@code rootDir}: the root (parent) module itself, plus
   * every module its {@code pom.xml} declares under {@code <modules>}.
   *
   * <p>Fails fast: if the root or any declared child module fails to scan, the whole call fails,
   * carrying that module's error — this deliberately does not invent a "partial success" model with
   * no concrete caller needing one yet. Does not recurse into a module's own nested {@code
   * <modules>} (multi-level aggregation) — no real project on hand needs that yet either.
   *
   * @param rootDir the multi-module project's root directory, containing the parent {@code pom.xml}
   * @return one snapshot per module (root first, then each declared child in declaration order), or
   *     a failure describing which module could not be scanned
   */
  public static Result<List<RepositorySnapshot>, PlatformError> scanWorkspace(Path rootDir) {
    Validation.requireNonNull(rootDir, "rootDir must not be null");

    Result<RepositorySnapshot, PlatformError> rootResult = scan(rootDir);
    if (rootResult.isFailure()) {
      return Result.failure(rootResult.fold(v -> null, error -> error));
    }

    Path rootPomPath = rootDir.resolve("pom.xml");
    Element rootProject;
    try {
      rootProject = parsePomXml(rootPomPath);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      // scan() above already parsed this file successfully; a failure here would mean it changed
      // on disk between the two reads. Treat identically to any other unparseable pom.
      return Result.failure(
          InfrastructureError.of(
              "repository.scan.pom_unparseable", "Could not parse " + rootPomPath, e));
    }

    List<RepositorySnapshot> snapshots = new ArrayList<>();
    snapshots.add(rootResult.fold(value -> value, error -> null));

    for (String moduleName : readModuleNames(rootProject)) {
      Result<RepositorySnapshot, PlatformError> moduleResult = scan(rootDir.resolve(moduleName));
      if (moduleResult.isFailure()) {
        return Result.failure(moduleResult.fold(v -> null, error -> error));
      }
      snapshots.add(moduleResult.fold(value -> value, error -> null));
    }

    return Result.success(List.copyOf(snapshots));
  }

  private static List<String> readModuleNames(Element project) {
    Element modulesElement = directChild(project, "modules");
    if (modulesElement == null) {
      return List.of();
    }
    NodeList children = modulesElement.getChildNodes();
    List<String> names = new ArrayList<>();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i) instanceof Element element && element.getTagName().equals("module")) {
        names.add(element.getTextContent().trim());
      }
    }
    return names;
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

  private static Integer readJavaVersion(Element project, Path moduleRoot) {
    Integer ownVersion = readJavaVersionFromOwnProperties(project);
    if (ownVersion != null) {
      return ownVersion;
    }

    // Not declared here — a properly-structured multi-module project typically declares it once,
    // on the parent, and lets every child inherit it (verified: DLMP's child modules have no
    // <properties> at all). Only follow a *local* parent (a relativePath pointing at a real file
    // on disk); a remote parent (Maven Central coordinates, <relativePath/> left empty) has
    // nothing on this filesystem to read.
    Path parentPomPath = resolveLocalParentPomPath(project, moduleRoot);
    if (parentPomPath == null || !Files.isRegularFile(parentPomPath)) {
      return null;
    }
    try {
      return readJavaVersionFromOwnProperties(parsePomXml(parentPomPath));
    } catch (ParserConfigurationException | SAXException | IOException e) {
      return null;
    }
  }

  private static Integer readJavaVersionFromOwnProperties(Element project) {
    // directChild, not getElementsByTagName — a <profile> can legitimately carry its own
    // <properties> block; only <project>'s own direct child is the module's actual, always-applied
    // properties.
    Element properties = directChild(project, "properties");
    if (properties == null) {
      return null;
    }
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
   * Resolves a coordinate ({@code groupId} or {@code version}) either from {@code project}
   * directly, or — since Maven lets a module inherit either from its {@code <parent>} — from the
   * {@code <parent>} element's own declared value. {@code artifactId} is never inherited and must
   * not be passed here.
   */
  private static String resolveInheritedCoordinate(Element project, String tagName) {
    String direct = childText(project, tagName);
    if (direct != null) {
      return direct;
    }
    NodeList parentNodes = project.getElementsByTagName("parent");
    if (parentNodes.getLength() == 0) {
      return null;
    }
    return childText((Element) parentNodes.item(0), tagName);
  }

  /**
   * Resolves the local filesystem path to {@code project}'s parent pom, if it declares one and that
   * parent is local (not purely a remote-repository coordinate).
   */
  private static Path resolveLocalParentPomPath(Element project, Path moduleRoot) {
    NodeList parentNodes = project.getElementsByTagName("parent");
    if (parentNodes.getLength() == 0) {
      return null;
    }
    Element parentElement = (Element) parentNodes.item(0);
    String relativePath = childText(parentElement, "relativePath");
    if ("".equals(relativePath)) {
      // An explicit, empty <relativePath/> means "no local parent" — Maven's own convention for
      // a parent that lives only in a remote repository (e.g. spring-boot-starter-parent).
      return null;
    }

    Path target =
        moduleRoot.resolve(relativePath != null ? relativePath : "../pom.xml").normalize();
    return Files.isDirectory(target) ? target.resolve("pom.xml") : target;
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
    List<ImportStatement> imports = new ArrayList<>();

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
    List<ImportStatement> imports = new ArrayList<>();
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
