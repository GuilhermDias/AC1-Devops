package org.example.ac1devops.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

/**
 * Regras do Monolito Modular (ADR-001) verificadas a cada build. Cada modulo de
 * {@link #MODULES} e um pacote {@code org.example.ac1devops.<modulo>} com {@code api/} publica e
 * {@code internal/} privada; o restante do sistema so enxerga a {@code api/} de cada modulo.
 * Um modulo novo so precisa ser acrescentado a {@link #MODULES} (e as camadas, em
 * {@link #camadasRespeitadas}).
 */
@AnalyzeClasses(packages = "org.example.ac1devops", importOptions = ImportOption.DoNotIncludeTests.class)
class ModularArchitectureTest {

    private static final String ROOT = "org.example.ac1devops";

    static final List<String> MODULES = List.of("progression", "ranking");

    private static String module(String name) {
        return ROOT + "." + name + "..";
    }

    private static String api(String name) {
        return ROOT + "." + name + ".api..";
    }

    private static String internal(String name) {
        return ROOT + "." + name + ".internal..";
    }

    @ArchTest
    static void ninguemForaDoModuloAcessaInternal(JavaClasses classes) {
        for (String name : MODULES) {
            noClasses()
                    .that().resideOutsideOfPackage(module(name))
                    .should().dependOnClassesThat().resideInAPackage(internal(name))
                    .because("o restante do sistema deve usar apenas a API publica do modulo " + name)
                    .check(classes);
        }
    }

    @ArchTest
    static void apiDoModuloNaoDependeDeInternal(JavaClasses classes) {
        for (String name : MODULES) {
            noClasses()
                    .that().resideInAPackage(api(name))
                    .should().dependOnClassesThat().resideInAPackage(ROOT + "..internal..")
                    .because("o contrato publico de " + name + " nao pode vazar detalhes de implementacao")
                    .check(classes);
        }
    }

    @ArchTest
    static void todoCodigoPertenceAUmModuloOuAoWebConfig(JavaClasses classes) {
        String[] allowed = new String[MODULES.size() + 3];
        allowed[0] = ROOT;
        allowed[1] = ROOT + ".web..";
        allowed[2] = ROOT + ".config..";
        for (int i = 0; i < MODULES.size(); i++) {
            allowed[i + 3] = module(MODULES.get(i));
        }
        classes()
                .should().resideInAnyPackage(allowed)
                .because("nao pode sobrar pacote legado por camada tecnica (service, repository, entity, dto...)")
                .check(classes);
    }

    @ArchTest
    static final ArchRule camadasRespeitadas = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Web").definedBy(ROOT + ".web..")
            .layer("ProgressionApi").definedBy(api("progression"))
            .layer("ProgressionInternal").definedBy(internal("progression"))
            .layer("RankingApi").definedBy(api("ranking"))
            .layer("RankingInternal").definedBy(internal("ranking"))
            .layer("Config").definedBy(ROOT + ".config..")
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Config").mayNotBeAccessedByAnyLayer()
            .whereLayer("ProgressionInternal").mayNotBeAccessedByAnyLayer()
            .whereLayer("RankingInternal").mayNotBeAccessedByAnyLayer()
            .whereLayer("ProgressionApi").mayOnlyBeAccessedByLayers(
                    "Web", "ProgressionInternal", "RankingApi", "RankingInternal")
            .whereLayer("RankingApi").mayOnlyBeAccessedByLayers("Web", "RankingInternal");

    @ArchTest
    static final ArchRule progressionNaoConheceRanking = noClasses()
            .that().resideInAPackage(module("progression"))
            .should().dependOnClassesThat().resideInAPackage(module("ranking"))
            .because("ranking depende de progression (pela api e por eventos), nunca o contrario");

    @ArchTest
    static final ArchRule dominioSemFramework = noClasses()
            .that().resideInAPackage(ROOT + ".progression.internal.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..",
                    ROOT + ".progression.internal.persistence..")
            .because("a regra de XP/nivel e POJO puro, independente de Spring e de JPA");

    @ArchTest
    static final ArchRule controllersFicamNaCamadaWeb = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage(ROOT + ".web..");

    @ArchTest
    static final ArchRule semCiclosEntrePacotes = slices()
            .matching(ROOT + ".(**)")
            .should().beFreeOfCycles();
}
