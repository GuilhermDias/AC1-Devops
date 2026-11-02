package org.example.ac1devops.recomendacao.internal.domain;

import java.util.List;

/**
 * Regra do comportamento degradado, sem Spring nem HTTP: sem resposta util da IA, o aluno recebe a
 * lista de populares e a resposta indica isso.
 */
public class RecommendationService {

    private final RecommendationPort recommendationPort;
    private final List<CourseRecommendation> fallback;

    public RecommendationService(RecommendationPort recommendationPort, List<CourseRecommendation> fallback) {
        this.recommendationPort = recommendationPort;
        this.fallback = List.copyOf(fallback);
    }

    public Recommendations recommend(StudentProfile profile) {
        try {
            List<CourseRecommendation> courses = recommendationPort.recommend(profile);
            return courses.isEmpty() ? new Recommendations(fallback, true) : new Recommendations(courses, false);
        } catch (RecommendationUnavailableException unavailable) {
            return new Recommendations(fallback, true);
        }
    }
}
