package org.example.ac1devops.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

/** As metricas que validam os ASRs precisam aparecer no endpoint que o Prometheus coleta (ADR-006). */
@AutoConfigureMetrics
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.endpoints.web.exposure.include=health,prometheus",
        "management.metrics.distribution.percentiles-histogram.http.server.requests=true",
        "cursos.outbox.metrics-interval-ms=100"
})
class ObservabilityIntegrationTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Porta fechada: a chamada ao servico de IA falha na hora, e a recomendacao degrada. */
    @DynamicPropertySource
    static void iaServiceDown(DynamicPropertyRegistry registry) throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            registry.add("ia.service.url", () -> "http://localhost:" + socket.getLocalPort());
        }
    }

    @Test
    void deveExporAsMetricasQueValidamOsAsrs() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            long id = objectMapper.readTree(post(client, "/students", "{\"name\":\"Aluno Metricas\"}").body())
                    .path("id").asLong();
            post(client, "/cursos/ci-cd/alunos/" + id + "/conclusao", "");
            get(client, "/ranking?page=0&size=5");
            get(client, "/ranking/top?n=3");
            get(client, "/ranking/students/" + id);
            get(client, "/recomendacoes/students/" + id);

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                HttpResponse<String> metrics = get(client, "/actuator/prometheus");
                assertThat(metrics.statusCode()).isEqualTo(200);
                assertThat(metrics.body())
                        .contains("http_server_requests_seconds_bucket")
                        .contains("ranking_query_seconds_bucket")
                        .contains("operation=\"page\"")
                        .contains("operation=\"position\"")
                        .contains("operation=\"top\"")
                        .contains("ia_recommendations_total{outcome=\"fallback\"")
                        .contains("outbox_events{status=\"DELIVERED\"")
                        .contains("conquistas_badges_awarded_total");
            });
        }
    }

    @Test
    void naoDeveExporEndpointsAlemDosNecessarios() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            assertThat(get(client, "/actuator/health").statusCode()).isEqualTo(200);
            assertThat(get(client, "/actuator/env").statusCode()).isEqualTo(404);
        }
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(HttpClient client, String path, String json) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
