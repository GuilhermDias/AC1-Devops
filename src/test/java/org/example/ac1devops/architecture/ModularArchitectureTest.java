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

    static final List<String> MODULES = List.of(
            "progression", "ranking", "conquistas", "recomendacao", "cursos", "notificacoes");

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
            .layer("ConquistasApi").definedBy(api("conquistas"))
            .layer("ConquistasInternal").definedBy(internal("conquistas"))
            .layer("RecomendacaoApi").definedBy(api("recomendacao"))
            .layer("RecomendacaoInternal").definedBy(internal("recomendacao"))
            .layer("CursosApi").definedBy(api("cursos"))
            .layer("CursosInternal").definedBy(internal("cursos"))
            .layer("NotificacoesApi").definedBy(api("notificacoes"))
            .layer("NotificacoesInternal").definedBy(internal("notificacoes"))
            .layer("Config").definedBy(ROOT + ".config..")
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Config").mayNotBeAccessedByAnyLayer()
            .whereLayer("ProgressionInternal").mayNotBeAccessedByAnyLayer()
            .whereLayer("RankingInternal").mayNotBeAccessedByAnyLayer()
            .whereLayer("ConquistasInternal").mayNotBeAccessedByAnyLayer()
            .whereLayer("RecomendacaoInternal").mayNotBeAccessedByAnyLayer()
            .whereLayer("CursosInternal").mayNotBeAccessedByAnyLayer()
            .whereLayer("NotificacoesInternal").mayNotBeAccessedByAnyLayer()
            .whereLayer("ProgressionApi").mayOnlyBeAccessedByLayers(
                    "Web", "ProgressionInternal", "RankingApi", "RankingInternal", "ConquistasInternal",
                    "RecomendacaoInternal", "CursosApi", "CursosInternal", "NotificacoesInternal")
            .whereLayer("RankingApi").mayOnlyBeAccessedByLayers("Web", "RankingInternal")
            .whereLayer("ConquistasApi").mayOnlyBeAccessedByLayers("Web", "ConquistasInternal")
            .whereLayer("RecomendacaoApi").mayOnlyBeAccessedByLayers("Web", "RecomendacaoInternal")
            .whereLayer("CursosApi").mayOnlyBeAccessedByLayers("Web", "CursosInternal", "NotificacoesInternal")
            .whereLayer("NotificacoesApi").mayOnlyBeAccessedByLayers("Web", "NotificacoesInternal");

    @ArchTest
    static final ArchRule progressionNaoConheceOutrosModulos = noClasses()
            .that().resideInAPackage(module("progression"))
            .should().dependOnClassesThat().resideInAnyPackage(
                    module("ranking"), module("conquistas"), module("recomendacao"),
                    module("cursos"), module("notificacoes"))
            .because("os outros modulos dependem de progression (pela api e por eventos), nunca o contrario");

    @ArchTest
    static final ArchRule dominioSemFrameworkNemAdapters = noClasses()
            .that().resideInAPackage(ROOT + "..internal.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "org.hibernate..",
                    ROOT + "..persistence..", ROOT + "..adapter..")
            .because("as regras de negocio sao POJOs puros, independentes de Spring, de JPA e dos adapters");

    /** Inversao de dependencia do ADR-002: os adapters conhecem a porta, nunca o contrario. */
    @ArchTest
    static void nadaDependeDosAdapters(JavaClasses classes) {
        for (String name : List.of("conquistas", "recomendacao", "notificacoes")) {
            noClasses()
                    .that().resideOutsideOfPackage(internal(name) + "adapter..")
                    .should().dependOnClassesThat().resideInAPackage(internal(name) + "adapter..")
                    .because("o acesso aos adapters e sempre pela porta, resolvida pelo Spring")
                    .check(classes);
        }
    }

    /** ADR-003: o fluxo essencial nao pode depender de IA, nem indiretamente pelo codigo. */
    @ArchTest
    static final ArchRule fluxoEssencialNaoConheceRecomendacao = noClasses()
            .that().resideInAnyPackage(module("ranking"), module("conquistas"), module("cursos"))
            .should().dependOnClassesThat().resideInAPackage(module("recomendacao"))
            .because("a indisponibilidade do servico de IA nao pode alcancar o fluxo essencial");

    /** ADR-004: quem conclui o curso nao conhece os consumidores do evento. */
    @ArchTest
    static final ArchRule cursosNaoConheceNotificacoes = noClasses()
            .that().resideInAPackage(module("cursos"))
            .should().dependOnClassesThat().resideInAPackage(module("notificacoes"))
            .because("a conclusao do curso nao pode depender do consumidor do evento");

    /** ADR-004: o consumidor so conhece o evento publicado, nao a outbox nem a transacao de conclusao. */
    @ArchTest
    static final ArchRule notificacoesSoConheceAApiDeCursos = noClasses()
            .that().resideInAPackage(module("notificacoes"))
            .should().dependOnClassesThat().resideInAPackage(internal("cursos"))
            .because("o consumidor recebe apenas CursoConcluidoEvent");

    @ArchTest
    static final ArchRule controllersFicamNaCamadaWeb = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage(ROOT + ".web..");

    @ArchTest
    static final ArchRule semCiclosEntrePacotes = slices()
            .matching(ROOT + ".(**)")
            .should().beFreeOfCycles();
}
