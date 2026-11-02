package org.example.ac1devops.recomendacao.internal.domain;

import java.util.List;

/** Porta para quem gera as recomendacoes. O dominio nao sabe que e um servico HTTP em Python. */
public interface RecommendationPort {

    /**
     * @throws RecommendationUnavailableException se nao for possivel obter as recomendacoes
     */
    List<CourseRecommendation> recommend(StudentProfile profile);
}
