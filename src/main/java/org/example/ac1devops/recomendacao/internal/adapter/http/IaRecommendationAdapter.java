package org.example.ac1devops.recomendacao.internal.adapter.http;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.net.http.HttpClient;
import java.util.List;
import org.example.ac1devops.recomendacao.internal.domain.CourseRecommendation;
import org.example.ac1devops.recomendacao.internal.domain.RecommendationPort;
import org.example.ac1devops.recomendacao.internal.domain.RecommendationUnavailableException;
import org.example.ac1devops.recomendacao.internal.domain.StudentProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter da {@link RecommendationPort} para o servico de IA (HTTP/JSON), com timeout curto e
 * circuit breaker. Qualquer falha vira {@link RecommendationUnavailableException}, e o dominio
 * decide o comportamento degradado.
 */
@Component
public class IaRecommendationAdapter implements RecommendationPort {

    private static final Logger log = LoggerFactory.getLogger(IaRecommendationAdapter.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public IaRecommendationAdapter(IaServiceProperties properties, CircuitBreaker circuitBreaker) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(requestFactory)
                .build();
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public List<CourseRecommendation> recommend(StudentProfile profile) {
        try {
            return circuitBreaker.executeSupplier(() -> requestRecommendations(profile));
        } catch (RuntimeException failure) {
            log.warn("Servico de IA indisponivel: {}", failure.getMessage());
            throw new RecommendationUnavailableException(failure);
        }
    }

    private List<CourseRecommendation> requestRecommendations(StudentProfile profile) {
        IaResponse response = restClient.post()
                .uri("/recomendacoes")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new IaRequest(profile.studentId(), profile.level().name()))
                .retrieve()
                .body(IaResponse.class);
        return response.courses().stream()
                .map(course -> new CourseRecommendation(course.courseId(), course.title(), course.reason()))
                .toList();
    }

    private record IaRequest(Long studentId, String level) {
    }

    private record IaResponse(List<IaCourse> courses) {
    }

    private record IaCourse(String courseId, String title, String reason) {
    }
}
