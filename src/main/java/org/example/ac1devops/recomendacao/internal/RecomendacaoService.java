package org.example.ac1devops.recomendacao.internal;

import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.example.ac1devops.progression.api.StudentScoreDTO;
import org.example.ac1devops.recomendacao.api.CourseDTO;
import org.example.ac1devops.recomendacao.api.RecomendacaoApi;
import org.example.ac1devops.recomendacao.api.RecommendationsDTO;
import org.example.ac1devops.recomendacao.internal.domain.RecommendationService;
import org.example.ac1devops.recomendacao.internal.domain.Recommendations;
import org.example.ac1devops.recomendacao.internal.domain.StudentProfile;
import org.springframework.stereotype.Service;

/** Implementacao de {@link RecomendacaoApi}: busca o aluno pela API de progressao e consulta o dominio. */
@Service
public class RecomendacaoService implements RecomendacaoApi {

    private final StudentProgressionApi studentProgression;
    private final RecommendationService recommendationService;

    public RecomendacaoService(StudentProgressionApi studentProgression, RecommendationService recommendationService) {
        this.studentProgression = studentProgression;
        this.recommendationService = recommendationService;
    }

    @Override
    public RecommendationsDTO getRecommendations(Long studentId) {
        StudentScoreDTO student = studentProgression.getScore(studentId);
        Recommendations recommendations = recommendationService.recommend(
                new StudentProfile(student.id(), student.level()));
        return new RecommendationsDTO(
                student.id(),
                recommendations.courses().stream()
                        .map(course -> new CourseDTO(course.courseId(), course.title(), course.reason()))
                        .toList(),
                recommendations.degraded());
    }
}
