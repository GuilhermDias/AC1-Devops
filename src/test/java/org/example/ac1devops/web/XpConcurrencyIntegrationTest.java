package org.example.ac1devops.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Requisicoes simultaneas de XP no mesmo aluno: nenhuma concessao pode se perder, e cada faixa
 * atravessada gera um unico level-up. HTTP real, para incluir a sessao do open-in-view.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class XpConcurrencyIntegrationTest {

    private static final int REQUESTS = 40;
    private static final int THREADS = 20;
    private static final int AMOUNT = 50;

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void naoDevePerderXpNemRepetirLevelUpSobConcorrencia() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient();
                ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            long id = objectMapper.readTree(post(client, "/students", "{\"name\":\"Concorrente\"}").body())
                    .path("id").asLong();

            List<Future<Integer>> statuses = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                statuses.add(pool.submit(() -> post(client, "/students/" + id + "/xp",
                        "{\"reason\":\"C\",\"amount\":" + AMOUNT + "}").statusCode()));
            }
            for (Future<Integer> status : statuses) {
                assertThat(status.get()).isEqualTo(200);
            }

            JsonNode student = objectMapper.readTree(get(client, "/students/" + id).body());
            assertThat(student.path("xpTotal").asInt()).isEqualTo(REQUESTS * AMOUNT);
            assertThat(student.path("level").asText()).isEqualTo("DIAMANTE");
            assertThat(student.path("levelUpEvents").size()).isEqualTo(3);

            JsonNode badges = objectMapper.readTree(get(client, "/conquistas/students/" + id).body());
            assertThat(badges.size()).isEqualTo(5);
        }
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
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
