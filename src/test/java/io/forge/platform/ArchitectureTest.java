package io.forge.platform;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces the package dependency rules stated in {@code CLAUDE.md} §8 and the kernel spec —
 * previously reviewed by hand only. Tracked as a known gap since Sprint 1 (see {@code
 * PROJECT_STATE.md}); closed here.
 */
@AnalyzeClasses(
    packages = "io.forge.platform",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest
  static final ArchRule coreDependsOnNothingButTheJdk =
      classes()
          .that()
          .resideInAPackage("io.forge.platform.core..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("io.forge.platform.core..", "java..", "javax..");

  @ArchTest
  static final ArchRule coreNeverDependsOnAi =
      noClasses()
          .that()
          .resideInAPackage("io.forge.platform.core..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("io.forge.platform.ai..");

  @ArchTest
  static final ArchRule coreNeverDependsOnIntelligence =
      noClasses()
          .that()
          .resideInAPackage("io.forge.platform.core..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("io.forge.platform.intelligence..");

  @ArchTest
  static final ArchRule aiNeverDependsOnIntelligence =
      noClasses()
          .that()
          .resideInAPackage("io.forge.platform.ai..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("io.forge.platform.intelligence..");

  @ArchTest
  static final ArchRule repositoryIntelligenceNeverDependsOnArchitectureIntelligence =
      noClasses()
          .that()
          .resideInAPackage("io.forge.platform.intelligence.repository..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("io.forge.platform.intelligence.architecture..");

  @ArchTest
  static final ArchRule topLevelPackagesAreFreeOfCycles =
      slices().matching("io.forge.platform.(*)..").should().beFreeOfCycles();

  @ArchTest
  static final ArchRule intelligenceSubpackagesAreFreeOfCycles =
      slices().matching("io.forge.platform.intelligence.(*)..").should().beFreeOfCycles();
}
