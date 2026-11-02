package org.example.ac1devops.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Fluxo real de RF02: o XP concedido em /students gera os badges, sem o modulo progression conhecer
 * o modulo conquistas. Evento -> dominio -> adapter JPA -> H2 -> GET /conquistas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConquistasEndpointIntegrationTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveConcederBadgesAoReceberXpESemDuplicarNoEventoSeguinte() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            long id = objectMapper.readTree(post(client, "/students", "{\"name\":\"Aluno Badges\"}").body())
                    .path("id").asLong();
            assertEquals(0, objectMapper.readTree(get(client, "/conquistas/students/" + id).body()).size());

            post(client, "/students/" + id + "/xp", "{\"reason\":\"DESAFIO\",\"amount\":350}");
            post(client, "/students/" + id + "/xp", "{\"reason\":\"DESAFIO\",\"amount\":10}");

            HttpResponse<String> response = get(client, "/conquistas/students/" + id);
            assertEquals(200, response.statusCode());
            JsonNode badges = objectMapper.readTree(response.body());
            assertEquals(3, badges.size());
            assertEquals("PRIMEIROS_PASSOS", badges.get(0).path("code").asText());
            assertEquals("NIVEL_PRATA", badges.get(1).path("code").asText());
            assertEquals("NIVEL_OURO", badges.get(2).path("code").asText());
            assertEquals("Alcancou o nivel Ouro", badges.get(2).path("description").asText());
        }
    }

    @Test
    void deveRetornar404ParaAlunoInexistente() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            assertEquals(404, get(client, "/conquistas/students/999999").statusCode());
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
