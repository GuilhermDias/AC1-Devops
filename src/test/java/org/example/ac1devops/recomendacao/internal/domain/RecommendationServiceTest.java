package org.example.ac1devops.recomendacao.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.ac1devops.progression.api.Level;
import org.junit.jupiter.api.Test;

/** Regra do modo degradado com portas falsas: sem Spring, sem HTTP. */
class RecommendationServiceTest {

    private static final StudentProfile PROFILE = new StudentProfile(1L, Level.PRATA);
    private static final CourseRecommendation COURSE = new CourseRecommendation("c1", "Curso", "Motivo");

    private static Recommendations recommendWith(RecommendationPort port) {
        return new RecommendationService(port, PopularCourses.defaults()).recommend(PROFILE);
    }

    @Test
    void deveDevolverARecomendacaoDaIaSemDegradar() {
        Recommendations result = recommendWith(profile -> List.of(COURSE));

        assertThat(result.courses()).containsExactly(COURSE);
        assertThat(result.degraded()).isFalse();
    }

    @Test
    void deveDegradarParaOsPopularesQuandoAIaEstaIndisponivel() {
        Recommendations result = recommendWith(profile -> {
            throw new RecommendationUnavailableException(new IllegalStateException("fora do ar"));
        });

        assertThat(result.courses()).isEqualTo(PopularCourses.defaults());
        assertThat(result.degraded()).isTrue();
    }

    @Test
    void deveDegradarQuandoAIaNaoTemNadaParaRecomendar() {
        Recommendations result = recommendWith(profile -> List.of());

        assertThat(result.courses()).isEqualTo(PopularCourses.defaults());
        assertThat(result.degraded()).isTrue();
    }
}
