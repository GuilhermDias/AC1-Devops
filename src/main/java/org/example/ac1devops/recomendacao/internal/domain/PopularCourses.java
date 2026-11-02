package org.example.ac1devops.recomendacao.internal.domain;

import java.util.List;

/**
 * Lista usada no modo degradado. Enquanto o Core nao tem cursos nem dados de popularidade, e uma
 * lista fixa proposta pela equipe.
 */
public final class PopularCourses {

    private static final String REASON = "Entre os cursos mais populares";

    private PopularCourses() {
    }

    public static List<CourseRecommendation> defaults() {
        return List.of(
                new CourseRecommendation("fundamentos-git", "Fundamentos de Git", REASON),
                new CourseRecommendation("testes-unitarios", "Testes Unitarios", REASON),
                new CourseRecommendation("docker-essencial", "Docker Essencial", REASON));
    }
}
