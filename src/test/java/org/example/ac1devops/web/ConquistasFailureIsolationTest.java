package org.example.ac1devops.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.example.ac1devops.conquistas.internal.domain.AwardedBadgePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Falha simulada: com a persistencia de conquistas fora do ar, o endpoint existente de XP continua
 * respondendo e salvando o XP. Conquistas nao pode derrubar a progressao do aluno.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConquistasFailureIsolationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private AwardedBadgePort awardedBadges;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void falhaEmConquistasNaoDeveAfetarAConcessaoDeXp() throws Exception {
        when(awardedBadges.findByStudent(any())).thenThrow(new IllegalStateException("persistencia fora do ar"));

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> created = post(client, "/students", "{\"name\":\"Aluno Falha\"}");
            assertEquals(201, created.statusCode());
            long id = objectMapper.readTree(created.body()).path("id").asLong();

            HttpResponse<String> updated = post(client, "/students/" + id + "/xp",
                    "{\"reason\":\"DESAFIO\",\"amount\":150}");

            assertEquals(200, updated.statusCode());
            JsonNode student = objectMapper.readTree(updated.body());
            assertEquals(150, student.path("xpTotal").asInt());
            assertEquals("PRATA", student.path("level").asText());
        }
    }

    private HttpResponse<String> post(HttpClient client, String path, String json) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
