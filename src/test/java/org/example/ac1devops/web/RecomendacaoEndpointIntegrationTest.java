package org.example.ac1devops.web;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Ponta a ponta: HTTP real no Core e no servico de IA (WireMock). A queda do servico nao pode afetar
 * as recomendacoes, que degradam, nem o fluxo essencial, que nao depende de IA.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecomendacaoEndpointIntegrationTest {

    private static final WireMockServer IA = new WireMockServer(wireMockConfig().dynamicPort());

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void iaServiceUrl(DynamicPropertyRegistry registry) {
        IA.start();
        registry.add("ia.service.url", IA::baseUrl);
    }

    @AfterAll
    static void stopIa() {
        if (IA.isRunning()) {
            IA.stop();
        }
    }

    @Test
    void deveDegradarQuandoOServicoCaiEManterOFluxoEssencial() throws Exception {
        IA.stubFor(post(urlEqualTo("/recomendacoes")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(Files.readString(
                        Path.of("labs/desafio-3/ia-service/contract/recomendacoes-resposta.json")))));

        try (HttpClient client = HttpClient.newHttpClient()) {
            long id = objectMapper.readTree(send(client, "POST", "/students", "{\"name\":\"Aluno IA\"}").body())
                    .path("id").asLong();

            JsonNode withIa = objectMapper.readTree(
                    send(client, "GET", "/recomendacoes/students/" + id, null).body());
            assertThat(withIa.path("degraded").asBoolean()).isFalse();
            assertThat(withIa.path("courses").get(0).path("courseId").asText()).isEqualTo("testes-unitarios");

            IA.stop();

            long start = System.nanoTime();
            HttpResponse<String> degraded = send(client, "GET", "/recomendacoes/students/" + id, null);
            long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

            assertThat(degraded.statusCode()).isEqualTo(200);
            assertThat(elapsedMillis).isLessThan(1_500);
            JsonNode body = objectMapper.readTree(degraded.body());
            assertThat(body.path("degraded").asBoolean()).isTrue();
            assertThat(body.path("courses").get(0).path("courseId").asText()).isEqualTo("fundamentos-git");

            HttpResponse<String> xp = send(client, "POST", "/students/" + id + "/xp",
                    "{\"reason\":\"DESAFIO\",\"amount\":150}");
            assertThat(xp.statusCode()).isEqualTo(200);
            assertThat(objectMapper.readTree(xp.body()).path("level").asText()).isEqualTo("PRATA");
            assertThat(send(client, "GET", "/students/" + id, null).statusCode()).isEqualTo(200);
            assertThat(send(client, "GET", "/ranking/students/" + id, null).statusCode()).isEqualTo(200);
            assertThat(send(client, "GET", "/conquistas/students/" + id, null).statusCode()).isEqualTo(200);
        }
    }

    @Test
    void deveRetornar404ParaAlunoInexistente() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            assertThat(send(client, "GET", "/recomendacoes/students/999999", null).statusCode()).isEqualTo(404);
        }
    }

    private HttpResponse<String> send(HttpClient client, String method, String path, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (json == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(json));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
