package org.example.ac1devops.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

/**
 * Regras do Monolito Modular (ADR-001) verificadas a cada build: o restante do sistema so
 * enxerga {@code progression.api}; {@code progression.internal} e privado ao modulo.
 */
@AnalyzeClasses(packages = "org.example.ac1devops", importOptions = ImportOption.DoNotIncludeTests.class)
class ModularArchitectureTest {

    private static final String MODULE = "org.example.ac1devops.progression..";
    private static final String MODULE_API = "org.example.ac1devops.progression.api..";
    private static final String MODULE_INTERNAL = "org.example.ac1devops.progression.internal..";

    @ArchTest
    static final ArchRule ninguemForaDoModuloAcessaInternal = noClasses()
            .that().resideOutsideOfPackage(MODULE)
            .should().dependOnClassesThat().resideInAPackage(MODULE_INTERNAL)
            .because("o restante do sistema deve usar apenas a API publica do modulo progression");

    @ArchTest
    static final ArchRule apiDoModuloNaoDependeDoInternal = noClasses()
            .that().resideInAPackage(MODULE_API)
            .should().dependOnClassesThat().resideInAPackage(MODULE_INTERNAL)
            .because("o contrato publico nao pode vazar detalhes de implementacao");

    @ArchTest
    static final ArchRule camadasRespeitadas = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Web").definedBy("org.example.ac1devops.web..")
            .layer("ProgressionApi").definedBy(MODULE_API)
            .layer("ProgressionInternal").definedBy(MODULE_INTERNAL)
            .layer("Config").definedBy("org.example.ac1devops.config..")
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Config").mayNotBeAccessedByAnyLayer()
            .whereLayer("ProgressionInternal").mayNotBeAccessedByAnyLayer()
            .whereLayer("ProgressionApi").mayOnlyBeAccessedByLayers("Web", "ProgressionInternal");

    @ArchTest
    static final ArchRule dominioSemFramework = noClasses()
            .that().resideInAPackage("org.example.ac1devops.progression.internal.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..",
                    "org.example.ac1devops.progression.internal.persistence..")
            .because("a regra de XP/nivel e POJO puro, independente de Spring e de JPA");

    @ArchTest
    static final ArchRule controllersFicamNaCamadaWeb = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("org.example.ac1devops.web..");

    @ArchTest
    static final ArchRule semCiclosEntrePacotes = slices()
            .matching("org.example.ac1devops.(**)")
            .should().beFreeOfCycles();
}
